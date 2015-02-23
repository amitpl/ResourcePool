package com.ap.jt;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class YAResourcePoolTest
{

    ResourcePool<String> rp;
    Executor             e = Executors.newFixedThreadPool(1);

    @BeforeMethod
    public void beforeTest()
    {
        rp = new YetAnotherResourcePool<String>();
        rp.open();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void acquireWithoutOpenTest()
    {
        rp.close();
        rp.acquire();
    }

    @Test
    public void addTwice()
    {
        Assert.assertEquals(rp.add("Monday"), true);
        Assert.assertEquals(rp.add("Monday"), false);
    }

    @Test
    public void acquireCorrectResource()
    {
        rp.add("Monday");
        Assert.assertEquals(rp.acquire(), "Monday");
    }

    @Test
    public void releaseCorrectResource()
    {
        rp.add("Monday");
        String r = rp.acquire();
        rp.release(r);
        Assert.assertEquals(rp.acquire(), "Monday");
    }

    @Test
    public void removeCorrectResource()
    {
        rp.add("Monday");
        rp.add("Tuesday");
        rp.remove("Monday");
        Assert.assertEquals(rp.acquire(), "Tuesday");
    }

    @Test(timeOut = 1000)
    public void blockOnAcquire()
    {
        e.execute(new Runnable() {

            public void run()
            {
                try
                {
                    Thread.sleep(500);
                }
                catch (InterruptedException e)
                {}
                rp.add("Monday");
            }
        });

        long s = System.nanoTime();
        String r = rp.acquire();
        long e = System.nanoTime();
        Assert.assertTrue(TimeUnit.NANOSECONDS.toMillis(e - s) >= 500);
        Assert.assertEquals(r, "Monday");
    }

    @Test
    public void timeoutOnAcquire()
    {
        String r = rp.acquire(500, TimeUnit.MILLISECONDS);
        Assert.assertNull(r);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void acquireAfterClose()
    {
        rp.add("Monday");
        rp.close();
        rp.acquire();
    }

    @Test
    public void addAcquiredResourceAgain()
    {
        rp.add("Monday");
        String r = rp.acquire();
        Assert.assertEquals(rp.add(r), false);
    }

    @Test(timeOut = 1000)
    public void timeoutAcquire()
    {
        e.execute(new Runnable() {

            public void run()
            {
                try
                {
                    Thread.sleep(700);
                }
                catch (InterruptedException e)
                {}
                rp.add("Monday");
            }
        });

        long s = System.nanoTime();
        String r = rp.acquire(500, TimeUnit.MILLISECONDS);
        long e = System.nanoTime();
        Assert.assertTrue(TimeUnit.NANOSECONDS.toMillis(e - s) >= 500);
        Assert.assertNull(r);
    }

    @Test
    public void removeNonManagedResource()
    {
        Assert.assertEquals(rp.remove("Monday"), false);
    }

    @Test
    public void removeManagedResource()
    {
        rp.add("Monday");
        Assert.assertEquals(rp.remove("Monday"), true);

    }

    @Test
    public void removeTwice()
    {
        rp.add("Monday");
        Assert.assertEquals(rp.remove("Monday"), true);
        Assert.assertEquals(rp.remove("Monday"), false);
    }

    @Test(timeOut = 1000)
    public void removeResourceWaitingForRelease()
    {
        rp.add("Monday");
        final String r = rp.acquire();
        e.execute(new Runnable() {

            public void run()
            {
                try
                {
                    Thread.sleep(500);
                }
                catch (InterruptedException e)
                {}
                rp.release(r);
            }
        });
        long s = System.nanoTime();
        boolean st = rp.remove("Monday");
        long e = System.nanoTime();
        Assert.assertTrue(TimeUnit.NANOSECONDS.toMillis(e - s) >= 500);
        Assert.assertEquals(st, true);

    }

    @Test
    public void removeNowWithoutWaitingForAcquiredResource()
    {
        rp.add("Monday");
        final String r = rp.acquire();
        e.execute(new Runnable() {

            public void run()
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {}
                rp.release(r);
            }
        });
        long s = System.nanoTime();
        boolean st = rp.removeNow("Monday");
        long e = System.nanoTime();
        Assert.assertTrue(TimeUnit.NANOSECONDS.toMillis(e - s) < 500);
        Assert.assertEquals(st, true);
    }

    @Test
    public void close()
    {
        rp.close();
        Assert.assertEquals(rp.isOpen(), false);
    }

    @Test
    public void closeWaitingForAcquiredResouceToRelease()
    {
        rp.add("Monday");
        rp.add("Tuesday");
        final String r1 = rp.acquire();
        final String r2 = rp.acquire();
        e.execute(new Runnable() {

            public void run()
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {}
                rp.release(r2);
                rp.release(r1);
            }
        });
        long s = System.nanoTime();
        rp.close();
        long e = System.nanoTime();
        Assert.assertTrue(TimeUnit.NANOSECONDS.toMillis(e - s) >= 1000);
    }

    @Test
    public void releaseResource()
    {
        rp.add("Monday");
        String r = rp.acquire();
        rp.release(r);
        Assert.assertEquals(rp.acquire(), "Monday");
    }

    @Test
    public void releaseNonManagedResource()
    {
        rp.release("Monday");
        rp.add("Tuesday");
        Assert.assertEquals(rp.acquire(), "Tuesday");
    }

}
