package com.jj.scribble_sdk_pen;

/**
 * Jay
 * 让当前线程等待或唤醒
 */

public class WaitGo {
    private final Object _monitor = new Object();
    private volatile boolean isWait;

    public void waitOne() throws InterruptedException {
        synchronized (_monitor) {
            while (!isWait) {
                _monitor.wait();
            }
            isWait = true;
        }
    }

    public boolean waitOne(long timeout) throws InterruptedException {
        synchronized (_monitor) {
            boolean result = false;
            long t = System.currentTimeMillis();
            while (!isWait) {
                _monitor.wait(timeout);
                // Check for timeout
                if (System.currentTimeMillis() - t >= timeout) {
                    break;
                } else {
                    result = true;
                }
            }
            isWait = true;
            return result;
        }
    }

    public void go() {
        synchronized (_monitor) {
            isWait = false;
            _monitor.notify();
        }
    }

    public void reset() {
        isWait = false;
    }

    public boolean isGo() {
        return !isWait;
    }
}