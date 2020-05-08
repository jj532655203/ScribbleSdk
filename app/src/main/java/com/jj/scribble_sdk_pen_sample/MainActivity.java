package com.jj.scribble_sdk_pen_sample;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.jj.scribble_sdk_pen.TransparentScribbleView;
import com.jj.scribble_sdk_pen.data.TouchPoint;
import com.jj.scribble_sdk_pen.data.TouchPointList;
import com.jj.scribble_sdk_pen.intf.RawInputCallback;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private SurfaceHolder.Callback holderCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");

            surfaceView.setRawInputCallback(inputCallBack);

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged");

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");

            surfaceView.setRawDrawingEnable(false);

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
    private TransparentScribbleView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(holderCallBack);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        surfaceView.post(new Runnable() {
            @Override
            public void run() {
                surfaceView.setRawDrawingEnable(true);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        surfaceView.setRawDrawingEnable(false);
    }

    public void clearScreen(View view) {
        surfaceView.clearScreenAfterSurfaceViewCreated();
    }

}
