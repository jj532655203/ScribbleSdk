package com.jj.scribble_sdk_pen_sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import com.jj.scribble_sdk_pen.PageTouchHelper;
import com.jj.scribble_sdk_pen.RawInputCallback;
import com.jj.scribble_sdk_pen.data.TouchPoint;
import com.jj.scribble_sdk_pen.data.TouchPointList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Bitmap surfaceViewTopBitmap;

    private SurfaceHolder.Callback callBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");

            BitmapFactory.Options ops = new BitmapFactory.Options();
            ops.outWidth = 1920;
            ops.outHeight = 1080;
            surfaceViewTopBitmap = BitmapFactory.decodeResource(surfaceView.getResources(), R.mipmap.exam, ops);
            if (!surfaceViewTopBitmap.isMutable()) {
                Bitmap newBitmap = surfaceViewTopBitmap.copy(Bitmap.Config.ARGB_8888, true);
                surfaceViewTopBitmap.recycle();
                surfaceViewTopBitmap = newBitmap;
            }

            PageTouchHelper.create(surfaceView, surfaceViewTopBitmap, inputCallBack).setRawDrawingEnabled(true);


            Paint renderPaint = new Paint();
            renderPaint.setStyle(Paint.Style.FILL);

            Canvas canvas = surfaceView.getHolder().lockCanvas();
            canvas.drawBitmap(surfaceViewTopBitmap, null, new Rect(0, 0, surfaceView.getWidth(), surfaceView.getHeight()), renderPaint);
            surfaceView.getHolder().unlockCanvasAndPost(canvas);

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged");

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");

        }
    };
    private RawInputCallback inputCallBack = new RawInputCallback() {
        @Override
        public void onBeginRawDrawing(TouchPoint var2) {
            Log.d(TAG, "onBeginRawDrawing");

        }

        @Override
        public void onEndRawDrawing(TouchPoint var2) {
            Log.d(TAG, "onEndRawDrawing");

        }

        @Override
        public void onRawDrawingTouchPointMoveReceived(TouchPoint var1) {
            Log.d(TAG, "onRawDrawingTouchPointMoveReceived");

        }

        @Override
        public void onRawDrawingTouchPointListReceived(TouchPointList var1) {
            Log.d(TAG, "onRawDrawingTouchPointListReceived");

        }
    };
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(callBack);

    }
}
