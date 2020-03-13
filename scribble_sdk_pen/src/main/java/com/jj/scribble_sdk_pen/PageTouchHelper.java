package com.jj.scribble_sdk_pen;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

import com.jj.scribble_sdk_pen.data.TouchPoint;
import com.jj.scribble_sdk_pen.data.TouchPointList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Jay
 * 1.实时显示笔迹
 * 3.回调笔迹线集合
 * 3.暂停/继续书写功能
 */
public class PageTouchHelper implements View.OnTouchListener {

    private static final String TAG = "PageTouchHelper";
    private static final int MOVE_OFFSET = 5;
    private final float widthRatio, heightRatio;
    private Bitmap surfaceViewTopBitmap;
    private SurfaceView surfaceView;
    private RawInputCallback callback;
    private WaitGo waitGo = new WaitGo();
    private boolean is2StopRender;
    private boolean isRenderRunning;
    private boolean isRefresh;
    private Paint canvasPaint, renderPaint;
    private static final float STROKE_WIDTH = 6f;
    private int lastPointsSize;

    @SuppressLint("ClickableViewAccessibility")
    private PageTouchHelper(SurfaceView surfaceView, Bitmap surfaceViewTopBitmap, RawInputCallback callback) {
        this.surfaceView = surfaceView;
        this.surfaceViewTopBitmap = surfaceViewTopBitmap;
        this.callback = callback;
        assert surfaceView != null;
        surfaceView.setOnTouchListener(this);
        initRenderPaint();
        initCanvasPaint();

        widthRatio = ((float) surfaceView.getWidth()) / surfaceViewTopBitmap.getWidth();
        heightRatio = ((float) surfaceView.getHeight()) / surfaceViewTopBitmap.getHeight();

    }

    /**
     * 本方法务必在surfaceViewCreated后调用
     */
    public static PageTouchHelper create(SurfaceView surfaceView, Bitmap surfaceViewTopBitmap, RawInputCallback callback) {
        if (surfaceView == null) {
            throw new IllegalArgumentException("surfaceView should not be null!");
        }/* else if (surfaceView.getHolder().isCreating()) {
            throw new IllegalArgumentException("please call method after surfaceView created!");
        }*/ else if (surfaceViewTopBitmap == null || surfaceViewTopBitmap.isRecycled() || !surfaceViewTopBitmap.isMutable()) {
            throw new IllegalArgumentException("surfaceViewTopBitmap invalid!");
        } else {
            return new PageTouchHelper(surfaceView, surfaceViewTopBitmap, callback);
        }
    }


    private static final int ACTIVE_POINTER_ID = 0;
    private List<TouchPoint> points = new ArrayList<>();
    private ConcurrentLinkedDeque<List<TouchPoint>> pathQueue = new ConcurrentLinkedDeque<>();

    /**
     * 监听surfaceView的motionEvent
     * 只监听第一根手指
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "onTouch");

        TouchPoint activeTouchPoint = new TouchPoint(event.getX(), event.getY());

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {

                points.clear();

                points.add(activeTouchPoint);
                renderPath();

                if (callback != null) callback.onBeginRawDrawing(activeTouchPoint);
            }
            break;
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                if (event.getPointerId(event.getActionIndex()) != ACTIVE_POINTER_ID) {
                    break;
                }

                points.add(activeTouchPoint);
                renderPath();

                TouchPointList touchPointList = new TouchPointList();
                touchPointList.getPoints().addAll(points);

                points.clear();

                if (callback != null) {
                    callback.onRawDrawingTouchPointListReceived(touchPointList);

                    callback.onEndRawDrawing(activeTouchPoint);
                }
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                int actionIndex = event.getActionIndex();
                if (event.getPointerId(actionIndex) != ACTIVE_POINTER_ID) {
                    break;
                }

                points.add(activeTouchPoint);

                //10个event绘制一遍
                if (points.size() > (lastPointsSize + MOVE_OFFSET)) {
                    renderPath();
                }

                if (callback != null) callback.onRawDrawingTouchPointMoveReceived(activeTouchPoint);
            }
            break;
        }
        return true;
    }


    public void setRawDrawingEnabled(boolean enable) {
        if (enable) {
            startRenderThread();
        } else {
            stopRenderThread();
        }
    }

    private synchronized void startRenderThread() {
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

    private void stopRenderThread() {
        if (!isRenderRunning) return;
        is2StopRender = true;
        waitGo.go();
    }

    private void initRenderPaint() {
        if (renderPaint != null) return;
        renderPaint = new Paint();
        renderPaint.setStrokeWidth(STROKE_WIDTH);
        renderPaint.setStyle(Paint.Style.STROKE);
        renderPaint.setColor(Color.BLACK);
    }

    private void initCanvasPaint() {
        if (canvasPaint != null) return;
        canvasPaint = new Paint();
        canvasPaint.setStyle(Paint.Style.FILL);
        canvasPaint.setColor(Color.WHITE);
    }

    private void doRender(List<TouchPoint> points) {

        long startDoRenderTime = System.currentTimeMillis();

        if (surfaceView.getHolder() == null) {
            Log.e(TAG, "doRender holder 为空 return");
            return;
        }
        if (!surfaceView.getHolder().getSurface().isValid()) {
            Log.e(TAG, "surfaceView released return");
            return;
        }

        Canvas canvas = null;
        try {
            canvas = surfaceView.getHolder().lockCanvas();

            //绘制本次笔迹
            if (points.isEmpty()) {
                return;
            }


            //添加新笔迹
            addAPath2Canvas(points, new Canvas(surfaceViewTopBitmap));

            //将新内容绘制到surfaceview
            canvas.drawColor(Color.TRANSPARENT);
            canvas.drawBitmap(surfaceViewTopBitmap, null, new Rect(0, 0, surfaceView.getWidth(), surfaceView.getHeight()), renderPaint);

            Log.d(TAG, "doRender consume time=" + (System.currentTimeMillis() - startDoRenderTime));
        } catch (Exception e) {
            Log.e(TAG, "doRender e=" + Log.getStackTraceString(e));
        } finally {
            if (canvas != null) {
                surfaceView.getHolder().unlockCanvasAndPost(canvas);
            }
        }

    }

    private void addAPath2Canvas(List<TouchPoint> points, Canvas canvas) {
        long startTime = System.currentTimeMillis();

        convert(points);

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

    private void convert(List<TouchPoint> points) {
        for (TouchPoint point : points) {
            point.x = point.x / widthRatio;
            point.y = point.y / heightRatio;
        }
    }

    private void renderPath() {
        Log.d(TAG, "renderPath start time=" + System.currentTimeMillis());

        lastPointsSize = points.size();

        List<TouchPoint> pathPoints = new ArrayList<>();
        try {
            for (TouchPoint point : points) {
                pathPoints.add(point.clone());
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            Log.e(TAG, "renderPath clone failed");
        }
        pathQueue.add(pathPoints);
        isRefresh = true;
        if (!waitGo.isGo()) waitGo.go();
    }


}
