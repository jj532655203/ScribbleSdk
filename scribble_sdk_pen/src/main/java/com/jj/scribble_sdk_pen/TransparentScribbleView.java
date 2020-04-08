package com.jj.scribble_sdk_pen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jj.scribble_sdk_pen.data.TouchPoint;
import com.jj.scribble_sdk_pen.data.TouchPointList;
import com.jj.scribble_sdk_pen.intf.RawInputCallback;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TransparentScribbleView extends SurfaceView {

    private static final String TAG = "TransparentScribbleView";
    private static final int FRAME_CACHE_SIZE = 48;
    private WaitGo renderWaitGo = new WaitGo();
    private WaitGo eraserWaitGo = new WaitGo();
    private boolean is2StopRender, is2StopEraser;
    private boolean isRenderRunning, isEraserRunning;
    private boolean isRendering, isErase;
    private Paint renderPaint;
    private float strokeWidth = 12f;
    private int strokeColor = Color.BLACK;
    private RawInputCallback rawInputCallback;
    private static final int ACTIVE_POINTER_ID = 0;
    private TouchPointList activeTouchPointList = new TouchPointList();
    private ConcurrentLinkedQueue<TouchPointList> last16PathQueue = new ConcurrentLinkedQueue<>();

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


    synchronized void startRenderThread() {
        is2StopRender = false;
        if (isRenderRunning) {
            return;
        }
        isRenderRunning = true;

        JobExecutor.getInstance().execute(new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "startRenderThread ThreadName=" + Thread.currentThread().getName());

                while (!is2StopRender) {

                    Canvas canvas = null;
                    try {
                        if (!isRendering) {
                            renderWaitGo.wait1();
                            continue;
                        }
                        isRendering = false;


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

                isRenderRunning = false;
                Log.d(TAG, "startRenderThread 停止笔划渲染线程成功 ThreadName=" + Thread.currentThread().getName());
            }
        });
    }


    synchronized void startEraserThread() {
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

                isRenderRunning = false;
                Log.d(TAG, "startEraserThread 停止擦除线程成功 ThreadName=" + Thread.currentThread().getName());
            }
        });
    }

    synchronized void stopRenderThread() {
        if (!isRenderRunning) return;
        is2StopRender = true;
        renderWaitGo.go();
    }

    synchronized void stopEraserThread() {
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

        isRendering = true;
        renderWaitGo.go();
    }


    /**
     * start scribble thread when onResume!stop it when onPause!
     *
     * @param enable true to start,otherwise to top.
     */
    public void setRawDrawingEnable(boolean enable) {
        Log.d(TAG, "setRawDrawingEnable enable=" + enable);

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
        if (!isRenderRunning) {
            Log.e(TAG, "reproduceScribblesAfterSurfaceRecreated --> need setRawDrawingEnable(true) first!");
            return;
        }
        isRendering = true;
        renderWaitGo.go();
    }

}
