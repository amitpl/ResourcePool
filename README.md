Design Notes and Considerations:

1. Given that the resource pool has to be thread safe, I decided to use Blocking queue to maintain thread-safety using the semantics (of 
single resource to single thread) provided by the queue. Thus no two threads will end up acquiring the same resource.

2. Lets consider a scenario of a rogue client which uses same resource R in three threads (T1, T2, T3).
	T1 => pool.add(R)
	T2 => pool.release(R)
	T3 => pool.remove(R)
	
	In such scenario, thread safety is ensured by synchronizing each of add, release, acquire, remove methods. 
	There are two ways to achieve this synchronization:
	1. Use a dedicated lock for each resource to minimize contention. Thus for N resources there will be N locks. This will lead to minimum contention;
	
	2. Use a single lock for operations on all the resources. However, this would lead to maximum contention.
	3. Neither use single lock nor N locks but fewer number of locks and divide the resources into bucket with one lock per bucket. This is 
	similar to striped locking used in ConcurrentHashMap. Google Guava provides a implementation of Striped Locks and I choose to use.
	
	
	Note: Synchronizing on a per resource guarantees thread safety for multiple threads operating on a single resource.

3. ConcurrentHashMap is used to keep track of all the busy resources, while available resources are kept in a BlockingQueue.
4. Condition variables are used for the signaling mechanisms of the remove/release methods.
5. A signaler (synchronized) similar in spirit to something like a CountUpDownLatch is used to manage signaling between pool methods. 


Testing:

Unit tests are written using TestNG to test out the functionality.


Improvements:
1. A test suite could be written to test the multi-threaded parts of the resource pool.
2. Had the ResourcePool been bounded, It would have been to use Ring Buffers to store available/busy resources.
3. A better data-structure could be chalked out for such a ResourcePool. Basically, we need a DS that would do the following:
	a). FIFO semantics of queue.
	b). Ability of look-up any element in O(1) time.
	Such DS could be implemented using a Doubly Queue and keeping the references of each node in a Hashmap again the elements.
	
	ie. Reference R = Queue(Element)
		Map.put(Element, R)
4. It would be interesting to use Dynamic Proxies to wrap a given resource will more information (such as managedByPool).
