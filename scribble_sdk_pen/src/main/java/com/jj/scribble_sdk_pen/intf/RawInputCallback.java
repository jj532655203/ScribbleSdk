package com.jj.scribble_sdk_pen.intf;

import com.jj.scribble_sdk_pen.data.TouchPoint;
import com.jj.scribble_sdk_pen.data.TouchPointList;

public abstract class RawInputCallback {

    public RawInputCallback() {
    }

    public abstract void onBeginRawDrawing(TouchPoint var2);

    public abstract void onEndRawDrawing(TouchPoint var2);

    public abstract void onRawDrawingTouchPointMoveReceived(TouchPoint var1);

    public abstract void onRawDrawingTouchPointListReceived(TouchPointList var1);

}
