package com.ap.jt;

import java.util.concurrent.TimeUnit;

/**
 * This class defines the common methods for a resource pool which doesn't
 * depend on how the resources are actually managed and synchronized.
 * 
 * @author amitpal
 *
 * @param <R>
 */
public abstract class AbstractResourcePool<R> implements ResourcePool<R>
{
    /** keep track of pool state (closed, open) */
    private volatile boolean    closed = true;

    /** this is used to signal resource acquired/release or pool closed events */
    private volatile CountLatch signal = new CountLatch();

    public void open()
    {
        closed = false;
    }

    public boolean isOpen()
    {
        return !closed;
    }

    public void close()
    {
        if (closed) return;
        closed = true;
        signal.awaits();
    }

    public void closeNow()
    {
        closed = true;
    }

    public boolean add(R r)
    {
        if (!isOpen()) throw new IllegalStateException("Pool already closed.");
        return safeAdd(r);
    }

    public R acquire()
    {
        return acquire(false, 0, null);
    }

    public R acquire(long timeout, TimeUnit unit)
    {
        return acquire(true, timeout, unit);
    }

    private R acquire(boolean isTimed, long timeout, TimeUnit unit)
    {
        if (!isOpen()) throw new IllegalStateException("Pool already closed.");
        R retval = safeAcquire(isTimed, timeout, unit);
        if (retval != null) signal.countUp();
        return retval;
    }

    /**
     * Implementation of these abstract methods need not worry about checking
     * the overall state of the pool (isOpen) Also the task of signaling is
     * taken care of and need not to be worried about.
     */
    protected abstract boolean safeAdd(R r);

    protected abstract R safeAcquire(boolean isTimed, long timeout, TimeUnit unit);

    protected abstract void safeRelease(R r);

    protected abstract boolean safeRemove(R r, boolean waitForRelease);

    public void release(R r)
    {
        safeRelease(r);
        signal.countDown();
    }

    public boolean remove(R r)
    {
        if (!isOpen()) throw new IllegalStateException("Pool already closed.");
        return safeRemove(r, true);
    }

    public boolean removeNow(R r)
    {
        if (!isOpen()) throw new IllegalStateException("Pool already closed.");
        return safeRemove(r, false);
    }

    /**
     * This class atomically increments/decrements the count and has a awaits
     * method which waits until the count becomes zero. This is something
     * similar to a CountDownLatch with a functionality to increase the latch.
     * 
     * @author amitpal
     *
     */
    public static class CountLatch
    {
        Integer count = 0;

        public synchronized void countUp()
        {
            count++;
            if (count == 0) notifyAll();
        }

        public synchronized void countDown()
        {
            count--;
            if (count == 0) notifyAll();
        }

        public synchronized void awaits()
        {
            if (count != 0)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {}
            }
        }
    }

}
