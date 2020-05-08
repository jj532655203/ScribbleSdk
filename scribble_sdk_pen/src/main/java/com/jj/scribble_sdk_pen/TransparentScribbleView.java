package com.jj.scribble_sdk_pen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jj.scribble_sdk_pen.data.TouchPoint;
import com.jj.scribble_sdk_pen.data.TouchPointList;
import com.jj.scribble_sdk_pen.intf.RawInputCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TransparentScribbleView extends SurfaceView {

    private static final String TAG = "TransparentScribbleView";
    private static final int FRAME_CACHE_SIZE = 16;
    private static final int MSG_RENDER_PATH = 101;
    private static final int MSG_ERASE_PATH = 102;
    private Paint renderPaint;
    private float strokeWidth = 12f;
    private int strokeColor = Color.BLACK;
    private RawInputCallback rawInputCallback;
    private static final int ACTIVE_POINTER_ID = 0;
    private TouchPointList activeTouchPointList = new TouchPointList();
    private ConcurrentLinkedQueue<TouchPointList> newPathListQueue = new ConcurrentLinkedQueue<>();
    private HandlerThread mRenderThread, mEraserThread;
    private android.os.Handler mRenderHandler, mEraserHandler;
    private boolean is2StopRender;
    private volatile boolean mRawDrawingEnable;
    private Bitmap mBitmap;
    private Canvas mCanvas;

    public int getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public TransparentScribbleView setRawInputCallback(RawInputCallback rawInputCallback) {
        this.rawInputCallback = rawInputCallback;
        return this;
    }

    public TransparentScribbleView(Context context) {
        this(context, null);
    }

    public TransparentScribbleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransparentScribbleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setBackgroundResource(R.color.transparent);
        setZOrderOnTop(true);
        SurfaceHolder holder = getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);

        initRenderPaint();

    }


    /**
     * 监听surfaceView的motionEvent
     * 只监听第一根手指
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouch");

        TouchPoint activeTouchPoint = new TouchPoint(event.getX(), event.getY());

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {

                activeTouchPointList.getPoints().clear();
                activeTouchPointList.add(activeTouchPoint);
                renderPath();

                if (rawInputCallback != null) rawInputCallback.onBeginRawDrawing(activeTouchPoint);
            }
            break;
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                if (event.getPointerId(event.getActionIndex()) != ACTIVE_POINTER_ID) {
                    break;
                }

                activeTouchPointList.add(activeTouchPoint);
                renderPath();

                TouchPointList touchPointList = new TouchPointList(activeTouchPointList.size());
                touchPointList.addAll(activeTouchPointList);

                TouchPointList touchPointList2 = new TouchPointList(activeTouchPointList.size());
                touchPointList2.addAll(activeTouchPointList);
                newPathListQueue.add(touchPointList2);

                activeTouchPointList.getPoints().clear();

                if (rawInputCallback != null) {
                    rawInputCallback.onRawDrawingTouchPointListReceived(touchPointList);

                    rawInputCallback.onEndRawDrawing(activeTouchPoint);
                }
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                int actionIndex = event.getActionIndex();
                if (event.getPointerId(actionIndex) != ACTIVE_POINTER_ID) {
                    break;
                }

                activeTouchPointList.add(activeTouchPoint);

                renderPath();

                if (rawInputCallback != null)
                    rawInputCallback.onRawDrawingTouchPointMoveReceived(activeTouchPoint);
            }
            break;
        }
        return true;
    }


    void startRenderThread() {
        is2StopRender = false;
        mRenderThread = new HandlerThread("RenderThread");
        mRenderThread.start();
        mRenderHandler = new android.os.Handler(mRenderThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what != MSG_RENDER_PATH) return;

                if (newPathListQueue.isEmpty() && activeTouchPointList.getPoints().isEmpty()) {
                    Log.d(TAG, "handleMessage 队列处理完毕 return");
                    removeCallbacksAndMessages(null);
                    return;
                }

                Canvas canvas = null;
                try {

                    long startDoRenderTime = System.currentTimeMillis();

                    if (getHolder() != null && getHolder().getSurface().isValid()) {

                        canvas = getHolder().lockCanvas();

                        //将新笔迹绘制到bitmap中
                        for (int i = 0; i < newPathListQueue.size(); i++) {
                            if (is2StopRender) return;

                            TouchPointList poll = newPathListQueue.poll();
                            if (poll == null) continue;

                            addAPath2Canvas(poll.getPoints(), mCanvas);
                        }

                        //将活动笔迹绘制到bitmap中
                        ArrayList<TouchPoint> activePath = new ArrayList<>(activeTouchPointList.getPoints());
                        addAPath2Canvas(activePath, mCanvas);
                        if (is2StopRender) return;

                        //将bitmap绘制到canvas中
                        Rect dstRect = new Rect(0, 0, getWidth(), getHeight());
                        canvas.drawBitmap(mBitmap, null, dstRect, renderPaint);
                        if (is2StopRender) return;


                        Log.d(TAG, "doRender consume time=" + (System.currentTimeMillis() - startDoRenderTime));

                    } else {
                        Log.e(TAG, "surfaceView released return");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        getHolder().unlockCanvasAndPost(canvas);
                    }
                }
            }
        };
    }

    void startEraserThread() {
        mEraserThread = new HandlerThread("EraserThread");
        mEraserThread.start();
        mEraserHandler = new Handler(mEraserThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what != MSG_ERASE_PATH) return;

                long millis = System.currentTimeMillis();

                mBitmap.eraseColor(Color.TRANSPARENT);

                //由于surfaceView的双缓冲机制
                for (int i = 0; i < FRAME_CACHE_SIZE; i++) {
                    Canvas canvas = getHolder().lockCanvas();
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    getHolder().unlockCanvasAndPost(canvas);
                }

                Log.d(TAG, "doErase consuming time " + (System.currentTimeMillis() - millis));

            }
        };
    }

    void stopRenderThread() {
        if (mRenderThread != null) mRenderThread.quit();
        is2StopRender = true;
    }

    void stopEraserThread() {
        if (mEraserThread != null) mEraserThread.quit();
    }

    private void initRenderPaint() {
        if (renderPaint != null) return;
        renderPaint = new Paint();
        renderPaint.setStrokeWidth(strokeWidth);
        renderPaint.setStyle(Paint.Style.STROKE);
        renderPaint.setColor(strokeColor);
    }

    private void addAPath2Canvas(List<TouchPoint> points, Canvas canvas) {

        if (points == null || points.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        int size = points.size();
        if (size == 1 || size == 2) {
            TouchPoint touchPoint = points.get(0);
            if (size == 1) {
                canvas.drawPoint(touchPoint.x, touchPoint.y, renderPaint);
            } else {
                TouchPoint touchPoint1 = points.get(1);
                canvas.drawLine(touchPoint.x, touchPoint.y, touchPoint1.x, touchPoint1.y, renderPaint);
            }
        } else {

            Path activePath = new Path();
            TouchPoint touchPoint0 = points.get(0);
            activePath.moveTo(touchPoint0.x, touchPoint0.y);
            for (int i = 0; i < size - 1; i++) {
                TouchPoint touchPointi = points.get(i);
                TouchPoint touchPointiPlus = points.get(i + 1);
                activePath.quadTo(touchPointi.x, touchPointi.y, touchPointiPlus.x, touchPointiPlus.y);
            }
            canvas.drawPath(activePath, renderPaint);

        }

        Log.d(TAG, "addAPath2Canvas consume time=" + (System.currentTimeMillis() - startTime));

    }

    private void renderPath() {
        Log.d(TAG, "renderPath start");

        mRenderHandler.sendEmptyMessage(MSG_RENDER_PATH);
    }

    public boolean getRawDrawingEnable() {
        return mRawDrawingEnable;
    }

    /**
     * start scribble thread when SurfaceCreated/SurfaceReCreated!stop it when onPause!
     *
     * @param enable true to start,otherwise to top.
     */
    public synchronized void setRawDrawingEnableAfterSurfaceCreated(boolean enable) {
        if (enable == mRawDrawingEnable) return;
        Log.d(TAG, "setRawDrawingEnableAfterSurfaceCreated enable=" + enable);

        mRawDrawingEnable = enable;

        if (enable) {
            startRenderThread();
            startEraserThread();
            reproduceScribblesAfterSurfaceRecreated();
        } else {
            stopRenderThread();
            stopEraserThread();
        }
    }

    /**
     * when this TransparentScribbleView need swipe up(case : used to a new page),you need call this function .
     * call this function must surfaceCreated!
     */
    public void clearScreenAfterSurfaceViewCreated() {
        Log.d(TAG, "clearScreenAfterSurfaceViewCreated ");

        newPathListQueue.clear();
        activeTouchPointList.getPoints().clear();

        clearScreen();

    }

    private void clearScreen() {
        if (mEraserHandler != null) mEraserHandler.sendEmptyMessage(MSG_ERASE_PATH);
    }

    /**
     * reproduce scribbles after surface recreate,but need setRawDrawingEnableAfterSurfaceCreated(true) first;
     * setRawDrawingEnableAfterSurfaceCreated(true) is a perfect trigger,since at the time is onResumed(as well as surface created)
     */
    public void reproduceScribblesAfterSurfaceRecreated() {
        Log.d(TAG, "reproduceScribblesAfterSurfaceRecreated ");
        if (mRenderHandler == null) {
            Log.e(TAG, "reproduceScribblesAfterSurfaceRecreated --> need setRawDrawingEnableAfterSurfaceCreated(true) first!");
            return;
        }

        post(new Runnable() {
            @Override
            public void run() {

                Rect dstRect = new Rect(0, 0, getWidth(), getHeight());
                Canvas canvas = getHolder().lockCanvas();
                canvas.drawBitmap(mBitmap, null, dstRect, renderPaint);
                getHolder().unlockCanvasAndPost(canvas);

            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "onAttachedToWindow");
        post(new Runnable() {
            @Override
            public void run() {
                mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBitmap);
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
