package com.jj.scribble_sdk_pen;

/**
 * Jay
 * 让当前线程等待或唤醒
 */

public class WaitGo {
    private final Object _monitor = new Object();
    private volatile boolean isGo = false;

    public void waitOne() throws InterruptedException {
        synchronized (_monitor) {
            while (!isGo) {
                _monitor.wait();
            }
            isGo = false;
        }
    }

    public boolean waitOne(long timeout) throws InterruptedException {
        synchronized (_monitor) {
            boolean result = false;
            long t = System.currentTimeMillis();
            while (!isGo) {
                _monitor.wait(timeout);
                // Check for timeout
                if (System.currentTimeMillis() - t >= timeout) {
                    break;
                } else {
                    result = true;
                }
            }
            isGo = false;
            return result;
        }
    }

    public void go() {
        synchronized (_monitor) {
            isGo = true;
            _monitor.notify();
        }
    }

    public void reset() {
        isGo = false;
    }

    public boolean isGo() {
        return isGo;
    }
}