package com.jj.scribble_sdk_pen.data;

public class PageRange {
    public String startPosition;
    public String endPosition;

    public PageRange(String start, String end) {
        this.startPosition = start;
        this.endPosition = end;
    }

    public static PageRange create(String start, String end) {
        return new PageRange(start, end);
    }
}
