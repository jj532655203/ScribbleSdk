# ScribbleSdk-Pen


##  本项目中TransparentScribbleView为即时显示触控笔迹的透明层,使用surfaceview的双缓冲机制,手写(或笔写)非常流畅,长笔迹+大笔迹量情况下依然无延迟

##  使用TransparentScribbleView,代码耦合性非常低




##  用法

###  1.引入包

项目根目录gradle文件下: 
buildscript.repositories{maven { url "https://jitpack.io" }}

需要使用可书写透明层的module的gradle下:
implementation 'com.github.jj532655203:ScribbleSdkPen:1.0.10'

###  2.布局
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <ImageView
        android:id="@+id/img"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="fitXY"
        android:src="@mipmap/exam" />

    <com.jj.scribble_sdk_pen.TransparentScribbleView
        android:id="@+id/surface_view"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="@+id/img"
        app:layout_constraintLeft_toLeftOf="@+id/img"
        app:layout_constraintRight_toRightOf="@+id/img"
        app:layout_constraintTop_toTopOf="@+id/img" />

</androidx.constraintlayout.widget.ConstraintLayout>

###  3.代码开启可书写
		TransparentScribbleView  scribleView = findViewById(R.id.surface_view);
        scribleView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");

                scribleView.setRawInputCallback(inputCallBack).setRawDrawingEnable(true);

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged");

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");

            }
        });
		//从inputCallBack的回调方法中获取笔迹集合,你可保持这些笔迹集合用于恢复手绘图
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


###  4.另外
TransparentScribbleView支持设置线宽和颜色,内部绘制线程在不绘制时处于wait状态,做到了最小能耗,切换页时请将旧page关闭书写,新page开启书写。
觉得好请star！让大家都有动力继续开源吧！