package com.jj.scribble_sdk_pen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
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

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TransparentScribbleView extends SurfaceView {

    private static final String TAG = "TransparentScribbleView";
    private static final int FRAME_CACHE_SIZE = 48;
    private static final int MSG_RENDER_PATH = 101;
    private WaitGo eraserWaitGo = new WaitGo();
    private boolean is2StopRender, is2StopEraser;
    private boolean isEraserRunning;
    private boolean isErase;
    private Paint renderPaint;
    private float strokeWidth = 12f;
    private int strokeColor = Color.BLACK;
    private RawInputCallback rawInputCallback;
    private static final int ACTIVE_POINTER_ID = 0;
    private TouchPointList activeTouchPointList = new TouchPointList();
    private ConcurrentLinkedQueue<TouchPointList> last16PathQueue = new ConcurrentLinkedQueue<>();
    private HandlerThread mRenderThread;
    private android.os.Handler mRenderHandler;
    private volatile boolean mRawDrawingEnable;

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

                TouchPointList touchPointList = new TouchPointList();
                touchPointList.addAll(activeTouchPointList);

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

                if (last16PathQueue.isEmpty()) {
                    Log.d(TAG, "handleMessage last16PathQueue队列处理完毕 return");
                    removeCallbacksAndMessages(null);
                    return;
                }

                Canvas canvas = null;
                try {

                    long startDoRenderTime = System.currentTimeMillis();

                    if (getHolder() != null && getHolder().getSurface().isValid()) {

                        canvas = getHolder().lockCanvas();

                        //由于双缓冲机制,得绘制最近几根笔迹
                        for (TouchPointList lastPath : last16PathQueue) {
                            if (lastPath.size() == 0) {
                                continue;
                            }

                            if (is2StopRender) break;

                            addAPath2Canvas(lastPath.getPoints(), canvas);
                        }

                        Log.d(TAG, "doRender consume time=" + (System.currentTimeMillis() - startDoRenderTime) + "?last16PathQueue.size=" + last16PathQueue.size());

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
        is2StopEraser = false;
        if (isEraserRunning) {
            return;
        }
        isEraserRunning = true;

        JobExecutor.getInstance().execute(new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "startEraserThread ThreadName=" + Thread.currentThread().getName());

                while (!is2StopEraser) {

                    try {
                        if (!isErase) {
                            eraserWaitGo.wait1();
                            continue;
                        }
                        isErase = false;

                        long millis = System.currentTimeMillis();

                        for (int i = 0; i < FRAME_CACHE_SIZE; i++) {
                            Canvas canvas = getHolder().lockCanvas();
                            if (canvas != null) {
                                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                                getHolder().unlockCanvasAndPost(canvas);
                            } else {
                                Log.e(TAG, "clearScreenAfterSurfaceViewCreated 失败!");
                            }
                        }

                        Log.d(TAG, "doErase consuming time " + (System.currentTimeMillis() - millis));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                Log.d(TAG, "startEraserThread 停止擦除线程成功 ThreadName=" + Thread.currentThread().getName());
            }
        });
    }

    void stopRenderThread() {
        if (mRenderThread != null) mRenderThread.quit();
        is2StopRender = true;
    }

    void stopEraserThread() {
        if (!isEraserRunning) return;
        is2StopEraser = true;
        eraserWaitGo.go();
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

        if (last16PathQueue.size() == FRAME_CACHE_SIZE) {
            last16PathQueue.poll();
        }
        TouchPointList lastTouchPointList = new TouchPointList(activeTouchPointList.size());
        lastTouchPointList.addAll(activeTouchPointList);
        last16PathQueue.add(lastTouchPointList);

        mRenderHandler.sendEmptyMessage(MSG_RENDER_PATH);
    }

    public boolean getRawDrawingEnable() {
        return mRawDrawingEnable;
    }

    /**
     * start scribble thread when onResume!stop it when onPause!
     *
     * @param enable true to start,otherwise to top.
     */
    public synchronized void setRawDrawingEnable(boolean enable) {
        if (enable == mRawDrawingEnable) return;
        Log.d(TAG, "setRawDrawingEnable enable=" + enable);

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

        last16PathQueue.clear();
        activeTouchPointList.getPoints().clear();

        clearScreen();

    }

    private void clearScreen() {
        isErase = true;
        eraserWaitGo.go();
    }

    /**
     * reproduce scribbles after surface recreate,but need setRawDrawingEnable(true) first;
     * setRawDrawingEnable(true) is a perfect trigger,since at the time is onResumed(as well as surface created)
     */
    public void reproduceScribblesAfterSurfaceRecreated() {
        Log.d(TAG, "reproduceScribblesAfterSurfaceRecreated ");
        if (mRenderHandler == null) {
            Log.e(TAG, "reproduceScribblesAfterSurfaceRecreated --> need setRawDrawingEnable(true) first!");
            return;
        }
        mRenderHandler.sendEmptyMessage(MSG_RENDER_PATH);
    }

}
