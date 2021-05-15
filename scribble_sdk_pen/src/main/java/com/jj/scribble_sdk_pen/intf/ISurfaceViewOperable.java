package com.jj.scribble_sdk_pen.intf;

import android.graphics.Bitmap;

import com.jj.scribble_sdk_pen.data.TouchPoint;
import com.jj.scribble_sdk_pen.data.TouchPointList;

import java.util.List;

public interface ISurfaceViewOperable {

    void setRawInputCallback(RawInputCallback rawInputCallback);

    void motionDownPoint(TouchPoint point);

    void motionMovePoint(TouchPoint point);

    void motionUpPoint(TouchPoint point);

    void motionDownErasePoint(TouchPoint point);

    void motionMoveErasePoint(TouchPoint point);

    void motionUpErasePoint(TouchPoint point);

    void addPaths2Canvas(List<TouchPointList> paths);

    void setBgBmp(Bitmap bmp);

    void clearScreenAfterSurfaceViewCreated();
}
