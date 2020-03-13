package com.jj.scribble_sdk_pen.data;

import android.graphics.Matrix;
import android.graphics.PointF;

import org.nustaq.serialization.annotations.Flat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Flat
public class TouchPointList implements Serializable, Cloneable {
    @Flat
    private List<TouchPoint> a;

    public TouchPointList() {
        this.a = new ArrayList();
    }

    public TouchPointList(int size) {
        this.a = new ArrayList(size);
    }

    public final List<TouchPoint> getPoints() {
        return this.a;
    }

    public void setPoints(List<TouchPoint> list) {
        this.a = list;
    }

    public int size() {
        return this.a.size();
    }

    public TouchPoint get(int i) {
        return (TouchPoint) this.a.get(i);
    }

    public void add(TouchPoint touchPoint) {
        this.a.add(touchPoint);
    }

    public void add(int index, TouchPoint touchPoint) {
        this.a.add(index, touchPoint);
    }

    public void addAll(TouchPointList other) {
        this.a.addAll(other.getPoints());
    }

    public Iterator<TouchPoint> iterator() {
        return this.a.iterator();
    }

    public void scaleAllPoints(float scaleValue) {
        TouchPoint var3;
        for (Iterator var2 = this.a.iterator(); var2.hasNext(); var3.y *= scaleValue) {
            var3 = (TouchPoint) var2.next();
            var3.x *= scaleValue;
        }

    }

    public void scaleAllPoints(float sx, float sy) {
        TouchPoint var4;
        for (Iterator var3 = this.a.iterator(); var3.hasNext(); var4.y *= Math.abs(sy)) {
            var4 = (TouchPoint) var3.next();
            var4.x *= Math.abs(sx);
        }

    }

    public void translateAllPoints(float dx, float dy) {
        TouchPoint var4;
        for (Iterator var3 = this.a.iterator(); var3.hasNext(); var4.y += dy) {
            var4 = (TouchPoint) var3.next();
            var4.x += dx;
        }

    }

    public void rotateAllPoints(float rotateAngle, PointF originPoint) {
        Matrix var3 = new Matrix();
        var3.setRotate(rotateAngle, originPoint.x, originPoint.y);

        TouchPoint var5;
        float[] var6;
        for (Iterator var4 = this.a.iterator(); var4.hasNext(); var5.y = var6[1]) {
            var5 = (TouchPoint) var4.next();
            var6 = new float[]{var5.x, var5.y};
            var3.mapPoints(var6);
            var5.x = var6[0];
        }

    }

    public void mirrorAllPoints(MirrorType type, float translateDistance) {
        Matrix var3 = new Matrix();
        switch (type) {
            case XAxisMirror:
                var3.setScale(-1.0F, 1.0F);
                var3.postTranslate(translateDistance, 0.0F);
                break;
            case YAxisMirror:
                var3.setScale(1.0F, -1.0F);
                var3.postTranslate(0.0F, translateDistance);
        }

        TouchPoint var5;
        float[] var6;
        for (Iterator var4 = this.a.iterator(); var4.hasNext(); var5.y = var6[1]) {
            var5 = (TouchPoint) var4.next();
            var6 = new float[]{var5.x, var5.y};
            var3.mapPoints(var6);
            var5.x = var6[0];
        }

    }

    public Object clone() throws CloneNotSupportedException {
        TouchPointList var1 = new TouchPointList();
        if (!this.a.isEmpty()) {
            Iterator var2 = this.a.iterator();

            while (var2.hasNext()) {
                TouchPoint var3 = (TouchPoint) var2.next();
                var1.add(var3.clone());
            }
        }

        return var1;
    }
}
