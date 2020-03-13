package com.jj.scribble_sdk_pen.data;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import org.nustaq.serialization.annotations.Flat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Flat
public class TouchPoint implements Serializable, Cloneable {

    private static final String TAG = "TouchPoint";
    @Flat
    public float x;
    @Flat
    public float y;
    @Flat
    public float pressure;
    @Flat
    public float size;
    @Flat
    public long timestamp;

    public TouchPoint() {
    }

    public TouchPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public TouchPoint(float px, float py, float p, float s, long ts) {
        this.x = px;
        this.y = py;
        this.pressure = p;
        this.size = s;
        this.timestamp = ts;
    }

    public TouchPoint(MotionEvent motionEvent) {
        this.x = motionEvent.getX();
        this.y = motionEvent.getY();
        this.pressure = motionEvent.getPressure();
        this.size = motionEvent.getSize();
        this.timestamp = motionEvent.getEventTime();
    }

    public TouchPoint(TouchPoint source) {
        this.x = source.getX();
        this.y = source.getY();
        this.pressure = source.getPressure();
        this.size = source.getSize();
        this.timestamp = source.getTimestamp();
    }

    public void set(TouchPoint point) {
        this.x = point.x;
        this.y = point.y;
        this.pressure = point.pressure;
        this.size = point.size;
        this.timestamp = point.timestamp;
    }

    public void offset(int dx, int dy) {
        this.x += (float) dx;
        this.y += (float) dy;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getPressure() {
        return this.pressure;
    }

    public float getSize() {
        return this.size;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void normalize(PageInfo pageInfo) {
        this.x = (this.x - pageInfo.getDisplayRect().left) / pageInfo.getActualScale();
        this.y = (this.y - pageInfo.getDisplayRect().top) / pageInfo.getActualScale();
    }

    public void origin(PageInfo pageInfo) {
        this.x = this.x * pageInfo.getActualScale() + pageInfo.getDisplayRect().left;
        this.y = this.y * pageInfo.getActualScale() + pageInfo.getDisplayRect().top;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void scale(float scaleValue) {
        this.x *= scaleValue;
        this.y *= scaleValue;
    }

    public static TouchPoint create(MotionEvent motionEvent) {
        return new TouchPoint(motionEvent);
    }

    public TouchPoint clone() throws CloneNotSupportedException {
        TouchPoint var1 = (TouchPoint) super.clone();
        return var1;
    }

    public String toString() {
        return "x:" + this.x + " y:" + this.y;
    }

    public static TouchPoint fromHistorical(MotionEvent motionEvent, int i) {
        return new TouchPoint(motionEvent.getHistoricalX(i), motionEvent.getHistoricalY(i), motionEvent.getHistoricalPressure(i), motionEvent.getHistoricalSize(i), motionEvent.getHistoricalEventTime(i));
    }

    public static List<TouchPoint> mapTouchPoints(Matrix matrix, List<TouchPoint> touchPoints) {
        if (matrix == null) {
            return touchPoints;
        } else {
            float[] var2 = new float[2];
            float[] var3 = new float[2];
            ArrayList var4 = new ArrayList();

            for (int var5 = 0; var5 < touchPoints.size(); ++var5) {
                TouchPoint var6 = (TouchPoint) touchPoints.get(var5);
                var3[0] = var2[0] = var6.x;
                var3[1] = var2[1] = var6.y;
                matrix.mapPoints(var3, var2);
                TouchPoint var7 = new TouchPoint(var6);
                var7.x = var3[0];
                var7.y = var3[1];
                var4.add(var7);
            }

            return var4;
        }
    }

    public static float getPointAngle(TouchPoint start, TouchPoint end) {
        double var2 = Math.atan2((double) (end.x - start.x), (double) (start.y - end.y));
        return (float) (180.0D * var2 / 3.141592653589793D);
    }

    public static float getPointDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow((double) (x1 - x2), 2.0D) + Math.pow((double) (y1 - y2), 2.0D));
    }

    public static float[] getTransformRectPoints(RectF originRect, Matrix matrix) {
        boolean var2 = true;
        float[] var3 = new float[8];
        float[] var4 = new float[8];
        float[] var5 = new float[8];
        float[] var6 = new float[8];
        byte var7 = 0;
        int var13 = var7 + 1;
        var3[var7] = originRect.left;
        var3[var13++] = originRect.top;
        var3[var13++] = originRect.right;
        var3[var13++] = originRect.top;
        var3[var13++] = originRect.right;
        var3[var13++] = originRect.bottom;
        var3[var13++] = originRect.left;
        var3[var13++] = originRect.bottom;
        if (matrix == null) {
            return var3;
        } else {
            matrix.mapPoints(var4, var3);
            matrix.mapRect(originRect);
            Matrix var8 = new Matrix();
            var8.postRotate(90.0F, var4[0], var4[1]);
            var8.mapPoints(var5, var4);
            TouchPoint var9 = getIntersection(new TouchPoint(var4[0], var4[1]), new TouchPoint(var5[2], var5[3]), new TouchPoint(var4[4], var4[5]), new TouchPoint(var4[6], var4[7]));
            var8 = new Matrix();
            var5 = new float[8];
            var8.postRotate(90.0F, var4[4], var4[5]);
            var8.mapPoints(var5, var4);
            TouchPoint var10 = getIntersection(new TouchPoint(var4[4], var4[5]), new TouchPoint(var5[6], var5[7]), new TouchPoint(var4[0], var4[1]), new TouchPoint(var4[2], var4[3]));
            if (var9 != null && !originRect.contains(var9.x, var9.y) && var10 != null && !originRect.contains(var10.x, var10.y)) {
                var6[0] = var4[0];
                var6[1] = var4[1];
                var6[2] = var10.x;
                var6[3] = var10.y;
                var6[4] = var4[4];
                var6[5] = var4[5];
                var6[6] = var9.x;
                var6[7] = var9.y;
                return var6;
            } else {
                var8 = new Matrix();
                var5 = new float[8];
                var8.postRotate(90.0F, var4[2], var4[3]);
                var8.mapPoints(var5, var4);
                TouchPoint var11 = getIntersection(new TouchPoint(var5[4], var5[5]), new TouchPoint(var4[2], var4[3]), new TouchPoint(var4[6], var4[7]), new TouchPoint(var4[0], var4[1]));
                var8 = new Matrix();
                var5 = new float[8];
                var8.postRotate(90.0F, var4[6], var4[7]);
                var8.mapPoints(var5, var4);
                TouchPoint var12 = getIntersection(new TouchPoint(var4[6], var4[7]), new TouchPoint(var5[0], var5[1]), new TouchPoint(var4[2], var4[3]), new TouchPoint(var4[4], var4[5]));
                var6[0] = var11.x;
                var6[1] = var11.y;
                var6[2] = var4[2];
                var6[3] = var4[3];
                var6[4] = var12.x;
                var6[5] = var12.y;
                var6[6] = var4[6];
                var6[7] = var4[7];
                return var6;
            }
        }
    }

    public static TouchPoint getIntersection(TouchPoint a, TouchPoint b, TouchPoint c, TouchPoint d) {
        TouchPoint var4 = new TouchPoint(a);
        if (Math.abs(b.y - a.y) + Math.abs(b.x - a.x) + Math.abs(d.y - c.y) + Math.abs(d.x - c.x) == 0.0F) {
            if (c.x - a.x + (c.y - a.y) == 0.0F) {
                Log.d(TAG, "ABCD is the same point!");
            } else {
                Log.d(TAG, "AB is a point, CD is a point, and AC is different!");
            }

            return var4;
        } else if (Math.abs(b.y - a.y) + Math.abs(b.x - a.x) == 0.0F) {
            if ((a.x - d.x) * (c.y - d.y) - (a.y - d.y) * (c.x - d.x) == 0.0F) {
                Log.d(TAG, "A, B is a point, and on the CD line segment!");
            } else {
                Log.d(TAG, "A, B is a point, and not on the CD line segment!");
            }

            return var4;
        } else if (Math.abs(d.y - c.y) + Math.abs(d.x - c.x) == 0.0F) {
            if ((d.x - b.x) * (a.y - b.y) - (d.y - b.y) * (a.x - b.x) == 0.0F) {
                Log.d(TAG, "C, D is a point, and on the AB line segment!");
            } else {
                Log.d(TAG, "C, D is a point, and not on the AB line segment!");
            }

            return var4;
        } else if ((b.y - a.y) * (c.x - d.x) - (b.x - a.x) * (c.y - d.y) == 0.0F) {
            Log.d(TAG, "Line segments are parallel, no intersections!");
            return var4;
        } else {
            var4.x = ((b.x - a.x) * (c.x - d.x) * (c.y - a.y) - c.x * (b.x - a.x) * (c.y - d.y) + a.x * (b.y - a.y) * (c.x - d.x)) / ((b.y - a.y) * (c.x - d.x) - (b.x - a.x) * (c.y - d.y));
            var4.y = ((b.y - a.y) * (c.y - d.y) * (c.x - a.x) - c.y * (b.y - a.y) * (c.x - d.x) + a.y * (b.x - a.x) * (c.y - d.y)) / ((b.x - a.x) * (c.y - d.y) - (b.y - a.y) * (c.x - d.x));
            return var4;
        }
    }
}

