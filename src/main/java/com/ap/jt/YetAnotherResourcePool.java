package com.ap.jt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import com.google.common.util.concurrent.Striped;

/**
 * This class implements the abstract methods of AbstractResourcePool. <br>
 * These method are synchronized by using lock-buckets made available in Guava Striped. <br>
 * The available resources are pooled in a queue for faster polling while the busy resources are
 * pooled in a map for easier retrieval by resource key. <br>
 * 
 * @author amitpal
 *
 * @param <R>
 */
public class YetAnotherResourcePool<R> extends AbstractResourcePool<R>
{
    /** bookkeeping of all busy resources */
    private volatile ConcurrentMap<R, Condition> busy      = new ConcurrentHashMap<R, Condition>();
    
    /** pooling of all available resources */
    private volatile BlockingQueue<R>            available = new LinkedBlockingQueue<R>();
    
    /** bookkeeping of all bucket locks */
    private volatile Striped<Lock>               stripe    = Striped.lock(8);

    /**
     * Acquire a lock on this resource and add it to the available queue if it is 
     * already not present in available queue or busy map.
     */
    @Override
    protected boolean safeAdd(R r)
    {
        Lock lock = getResourceLock(r);
        lock.lock();
        try
        {
            if (busy.containsKey(r) || available.contains(r))
                return false;
            else
            {
                available.put(r);
                return true;
            }
        }
        catch (InterruptedException e)
        {
            return false;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Acquire a resource by dequeueing from the available queue
     * After getting this free resource, it acquires a lock on this resource to update the busy map.
     */
    @Override
    protected R safeAcquire(boolean isTimed, long timeout, TimeUnit unit)
    {
        R r = null;

        try
        {
            if (isTimed)
                r = available.poll(timeout, unit);
            else
                r = available.take();
        }
        catch (InterruptedException e)
        {}
        finally
        {
            if (r == null) return null;
        }

        Lock lock = getResourceLock(r);
        lock.lock();
        try
        {
            busy.put(r, lock.newCondition());
        }
        finally
        {
            lock.unlock();
        }
        return r;
    }

    /**
     * Acquire a lock on this resource, and remove it from the busy map, signaling any other thread waiting
     *  for removal of this resource from the pool
     */
    @Override
    protected void safeRelease(R r)
    {
        Lock lock = getResourceLock(r);
        lock.lock();
        try
        {
            if (busy.containsKey(r))
            {
                // let the threads waiting to remove this resource know that
                // this resource has been released.
                busy.remove(r).signalAll();
                available.put(r);
            }
        }
        catch (InterruptedException e)
        {}
        finally
        {
            lock.unlock();
        }

    }

    /**
     * Acquire a lock on this resource, and remove it from either available queue or waiting until it is removed from busy map.
     */
    @Override
    protected boolean safeRemove(R r, boolean waitForRelease)
    {
        Lock lock = getResourceLock(r);
        lock.lock();
        try
        {
            if (waitForRelease && busy.containsKey(r))
            {
                try
                {
                    busy.get(r).await();
                }
                catch (InterruptedException e)
                {}
            }

            return busy.remove(r) != null || available.remove(r);
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Get a lock corresponding to a resource.
     */
    private Lock getResourceLock(R r)
    {
        return stripe.get(r);
    }
}
