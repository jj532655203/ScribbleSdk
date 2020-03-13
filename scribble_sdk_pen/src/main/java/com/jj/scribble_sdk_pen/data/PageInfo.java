package com.jj.scribble_sdk_pen.data;

import android.graphics.Matrix;
import android.graphics.RectF;

import com.jj.scribble_sdk_pen.utils.StringUtils;

public class PageInfo {
    private String b;
    private PageRange c;
    private int d;
    private int e;
    private int f;
    private float g;
    private float h;
    private boolean i;
    private RectF j;
    private RectF k;
    private RectF l;
    private RectF m;
    private float n;
    private int o;
    private String p;

    public static PageInfo copyWithSubPage(PageInfo pageInfo) {
        PageInfo var1 = new PageInfo(pageInfo);
        var1.setSubPage(pageInfo.getSubPage());
        return var1;
    }

    public PageInfo() {
        this.d = 0;
        this.f = 0;
        this.k = new RectF();
        this.l = new RectF();
        this.m = new RectF();
        this.n = 1.0F;
        this.o = 0;
    }

    public PageInfo(String name, float nw, float nh) {
        this(name, name, name, nw, nh);
    }

    public PageInfo(String name, int subPage, float nw, float nh) {
        this(name, name, name, nw, nh);
        this.d = subPage;
    }

    public PageInfo(String name, String position, float nw, float nh) {
        this(name, position, position, nw, nh);
    }

    public PageInfo(String name, String startPosition, String endPosition, float nw, float nh) {
        this.d = 0;
        this.f = 0;
        this.k = new RectF();
        this.l = new RectF();
        this.m = new RectF();
        this.n = 1.0F;
        this.o = 0;
        this.b = name;
        this.c = new PageRange(startPosition, endPosition);
        this.g = nw;
        this.h = nh;
        this.k.set(0.0F, 0.0F, nw, nh);
    }

    public PageInfo(String name, String startPosition, String endPosition, int subPage, float nw, float nh) {
        this.d = 0;
        this.f = 0;
        this.k = new RectF();
        this.l = new RectF();
        this.m = new RectF();
        this.n = 1.0F;
        this.o = 0;
        this.b = name;
        this.c = new PageRange(startPosition, endPosition);
        this.d = subPage;
        this.g = nw;
        this.h = nh;
        this.k.set(0.0F, 0.0F, nw, nh);
    }

    public PageInfo(PageInfo pageInfo) {
        this.d = 0;
        this.f = 0;
        this.k = new RectF();
        this.l = new RectF();
        this.m = new RectF();
        this.n = 1.0F;
        this.o = 0;
        this.b = pageInfo.getName();
        this.c = new PageRange(pageInfo.getRange().startPosition, pageInfo.getRange().endPosition);
        this.g = pageInfo.getOriginWidth();
        this.h = pageInfo.getOriginHeight();
        this.k.set(pageInfo.k);
        this.l.set(pageInfo.l);
        this.m.set(pageInfo.m);
        this.n = pageInfo.n;
        this.o = pageInfo.o;
        this.i = pageInfo.i;
        this.p = pageInfo.p;
    }

    public final float getOriginWidth() {
        return this.g;
    }

    public final float getOriginHeight() {
        return this.h;
    }

    public boolean isTextPage() {
        return this.i;
    }

    public void setIsTextPage(boolean value) {
        this.i = value;
    }

    public final RectF getAutoCropContentRegion() {
        return this.j;
    }

    public void setAutoCropContentRegion(RectF region) {
        this.j = region;
    }

    public final RectF getPositionRect() {
        return this.k;
    }

    public final float getScaledHeight() {
        return this.k.height();
    }

    public final float getScaledWidth() {
        return this.k.width();
    }

    public final float getActualScale() {
        return this.n;
    }

    public void update(float newScale, float x, float y) {
        this.setScale(newScale);
        this.setPosition(x, y);
    }

    public void setPosition(float x, float y) {
        this.k.offsetTo(x, y);
    }

    public float getX() {
        return this.k.left;
    }

    public float getY() {
        return this.k.top;
    }

    public void setX(float x) {
        this.k.offsetTo(x, this.k.top);
    }

    public void setY(float y) {
        this.k.offsetTo(this.k.left, y);
    }

    public void setScale(float newScale) {
        this.n = newScale;
        this.k.set(this.k.left, this.k.top, this.k.left + this.g * this.n, this.k.top + this.h * this.n);
    }

    public RectF updateDisplayRect(RectF rect) {
        this.l = new RectF(rect);
        return this.l;
    }

    public RectF getDisplayRect() {
        return this.l;
    }

    public RectF updateVisibleRect(RectF rect) {
        this.m = new RectF(rect);
        return this.m;
    }

    public RectF getVisibleRect() {
        return this.m;
    }

    public int getPageDisplayOrientation() {
        return this.f;
    }

    public void setName(String n) {
        this.b = n;
    }

    public final String getName() {
        return this.b;
    }

    public int getPageNumber() {
        return StringUtils.isNullOrEmpty(this.b) ? -1 : Integer.parseInt(this.b);
    }

    public String getPositionSafely() {
        return StringUtils.isNullOrEmpty(this.c.startPosition) ? this.b : this.c.startPosition;
    }

    public String getPosition() {
        assert this.c != null;

        return this.c.startPosition;
    }

    public PageRange getRange() {
        return this.c;
    }

    public int getSubPage() {
        return this.d;
    }

    public void setSubPage(int subPage) {
        this.d = subPage;
    }

    public Matrix normalizeMatrix() {
        Matrix var1 = new Matrix();
        float var2 = 1.0F / this.getDisplayRect().width() / this.getActualScale();
        float var3 = 1.0F / this.getDisplayRect().height() / this.getActualScale();
        var1.postTranslate(-this.getDisplayRect().left, -this.getDisplayRect().top);
        var1.postScale(var2, var3);
        return var1;
    }

    public String getExtraKey() {
        return this.p;
    }

    public PageInfo setExtraKey(String extraKey) {
        this.p = extraKey;
        return this;
    }

    public PageInfo setOriginWidth(float originWidth) {
        this.g = originWidth;
        return this;
    }

    public PageInfo setOriginHeight(float originHeight) {
        this.h = originHeight;
        return this;
    }

    public String toString() {
        return "PageInfo{name='" + this.b + '\'' + ", subPage=" + this.d + '}';
    }
}
