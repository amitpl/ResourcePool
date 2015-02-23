package com.ap.jt;

import java.util.concurrent.TimeUnit;

public interface ResourcePool<R> {
	void open();
	boolean isOpen();
	void close();
	void closeNow();
	boolean add(R resource);
	boolean remove(R resource);
	boolean removeNow(R resource);
	R acquire();
	R acquire(long timeout, TimeUnit unit);
	void release(R resource);
}
