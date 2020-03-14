package com.jj.scribble_sdk_pen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;

import com.jj.scribble_sdk_pen.data.TouchPoint;
import com.jj.scribble_sdk_pen.data.TouchPointList;
import com.jj.scribble_sdk_pen.intf.RawInputCallback;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TransparentScribbleView extends SurfaceView {

    private static final String TAG = "TransparentScribbleView";
    private static final int FRAME_CACHE_SIZE = 16;
    private WaitGo waitGo = new WaitGo();
    private boolean is2StopRender;
    private boolean isRenderRunning;
    private boolean isRefresh;
    private Paint renderPaint;
    private float strokeWidth = 12f;
    private int strokeColor = Color.BLACK;
    private RawInputCallback rawInputCallback;
    private static final int ACTIVE_POINTER_ID = 0;
    private TouchPointList activeTouchPointList = new TouchPointList();
    private ConcurrentLinkedDeque<TouchPointList> pathQueue = new ConcurrentLinkedDeque<>();
    private ConcurrentLinkedDeque<TouchPointList> last16PathQueue = new ConcurrentLinkedDeque<>();

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
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

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
                    try {
                        if (!isRefresh) {
                            waitGo.waitOne();
                            continue;
                        }
                        isRefresh = false;

                        doRender(pathQueue.pollFirst());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                isRenderRunning = false;
                Log.d(TAG, "startRenderThread 停止笔划渲染线程成功 ThreadName=" + Thread.currentThread().getName());
            }
        });
    }

    void stopRenderThread() {
        if (!isRenderRunning) return;
        is2StopRender = true;
        waitGo.go();
    }

    private void initRenderPaint() {
        if (renderPaint != null) return;
        renderPaint = new Paint();
        renderPaint.setStrokeWidth(strokeWidth);
        renderPaint.setStyle(Paint.Style.STROKE);
        renderPaint.setColor(strokeColor);
    }

    private void doRender(TouchPointList touchPointList) {
        List<TouchPoint> points = touchPointList.getPoints();

        long startDoRenderTime = System.currentTimeMillis();

        if (getHolder() == null) {
            Log.e(TAG, "doRender holder 为空 return");
            return;
        }
        if (!getHolder().getSurface().isValid()) {
            Log.e(TAG, "surfaceView released return");
            return;
        }

        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();

            //由于双缓冲机制,得绘制最近几根笔迹
            for (TouchPointList lastPath : last16PathQueue) {
                if (lastPath.size() == 0) {
                    continue;
                }

                addAPath2Canvas(lastPath.getPoints(), canvas);
            }

            //绘制本次笔迹
            if (points.isEmpty()) {
                return;
            }


            //添加新笔迹
            addAPath2Canvas(points, canvas);

            Log.d(TAG, "doRender consume time=" + (System.currentTimeMillis() - startDoRenderTime) + "?last16PathQueue.size=" + last16PathQueue.size() + "?pathQueue.size=" + pathQueue.size());
        } catch (Exception e) {
            Log.e(TAG, "doRender e=" + Log.getStackTraceString(e));
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }

    }

    private void addAPath2Canvas(List<TouchPoint> points, Canvas canvas) {
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
            last16PathQueue.removeFirst();
        }
        TouchPointList lastTouchPointList = new TouchPointList(activeTouchPointList.size());
        lastTouchPointList.addAll(activeTouchPointList);
        last16PathQueue.add(lastTouchPointList);

        TouchPointList touchPointList = new TouchPointList(activeTouchPointList.size());
        touchPointList.addAll(activeTouchPointList);

        pathQueue.add(touchPointList);
        isRefresh = true;
        if (!waitGo.isGo()) waitGo.go();
    }


    public void setRawDrawingEnable(boolean enable) {
        if (enable) {
            startRenderThread();
        } else {
            stopRenderThread();
        }
    }
}
