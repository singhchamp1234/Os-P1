package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;
        
        // Condition2 constraint: no semaphores -> using LinkedList/KThread
        //initialized in KThread
        readyQueue = new LinkedList<KThread>(); 
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        
        // interrupt disabled with lock
        boolean intStatus = Machine.interrupt().disable();
        
        // lock released
        conditionLock.release();
        
        // currentThread added to readyQueue (consists of waiting threads)
        readyQueue.add(KThread.currentThread());
        
        // currentThread goes to sleep
        KThread.sleep();
        
        // lock acquired
        conditionLock.acquire();
        
        // interrupt enabled
        Machine.interrupt().restore(intStatus);
	
	
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        
        // interrupt disabled with lock
        boolean intStatus = Machine.interrupt().disable();
        
        // check if readyQueue is not empty
        if(!readyQueue.isEmpty()) {	
            
            // remove first thread from readyQueue and add to the end of threads to be implemented
            ((KThread) readyQueue.removeFirst()).ready();
            
        }
	
		// interrupt re-enabled
		Machine.interrupt().restore(intStatus);
		
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
            
        // adapted from Condition.java which does not use semaphores
        while(!readyQueue.isEmpty()) 
            wake();
    }
    
    private Lock conditionLock;
    private LinkedList<KThread> readyQueue;
}
