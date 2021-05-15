package com.jj.scribble_sdk_pen_sample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.jj.scribble_sdk_pen.TransparentScribbleView;
import com.jj.scribble_sdk_pen.data.TouchPoint;
import com.jj.scribble_sdk_pen.data.TouchPointList;
import com.jj.scribble_sdk_pen.intf.RawInputCallback;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
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
        surfaceView.setRawInputCallback(inputCallBack);

    }

    public void clearScreen(View view) {
        surfaceView.clearScreenAfterSurfaceViewCreated();
    }

}
