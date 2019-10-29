package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 * <p/>
 * <p/>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 * <p/>
 * <p/>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 * <p/>
 * <p/>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks,` and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param transferPriority <tt>true</tt> if this queue should
	 *                         transfer priority from waiting threads
	 *                         to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}
// Get the priority of the specified thread that we are looking at
	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}
// Get the effective priority of the specified thread needed.
	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}
// Set the priority of the specified thread. that we are currently looking at


	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum &&
			priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}
// If possible, lower the priority of the current thread user in some scheduler-dependent way, preferably by the same amount as would a call to increasePriority(
	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState)thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
// transferPriority - true if this queue should transfer priority 
// from the waiting threads, to the current owning thread available
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			this.threadsList = new LinkedList<ThreadState>();
		}
// Return the scheduling state of the specified thread.
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			final ThreadState threadStateObject = getThreadState(thread);
			this.threadsList.add(threadStateObject);
			threadStateObject.waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			final ThreadState threadStateObject = getThreadState(thread);
			if (this.resourceHolder != null)
			{
				this.resourceHolder.release(this);
			}
			this.resourceHolder = threadStateObject;
			threadStateObject.acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			//creating an object of next thread
			final ThreadState threadStateObject = this.pickNextThread();

			if (threadStateObject == null) return null;


			this.threadsList.remove(threadStateObject);  //removing thread from queue

			this.acquire(threadStateObject.getThread()); //acquie thread

			return threadStateObject.getThread();
		}


		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return the next thread that <tt>nextThread()</tt> would
		 *         return.
		 */
		protected ThreadState pickNextThread()
		{

			//declare appropriate variables
			ThreadState threadStateObject = null;
			int minPriority = priorityMinimum;
			for (final ThreadState tempTS : this.threadsList)
			{
				int currPriority = tempTS.getEffectivePriority();
				if (threadStateObject == null || (currPriority > minPriority))
				{

					//updating values if condition satisfied
					minPriority = currPriority;
					threadStateObject = tempTS;
				}
			}
			return threadStateObject;    //returning object of thread state
		}


		public ThreadState peekNext()
		{
			//returning next thread
			return this.pickNextThread();
		}

		/**
		 * This method returns the effectivePriority of this PriorityQueue.
		 * The return value is cached for as long as possible. If the cached value
		 * has been invalidated, this method will spawn a series of mutually
		 * recursive calls needed to recalculate effectivePriorities across the
		 * entire resource graph.
		 * @return
		 */
		public int getEffectivePriority()
		{

			int minPriority = priorityMinimum;
			if (!this.transferPriority)       //check first condition if it is transfer priority or not
			{
				return minPriority;
			}
			else if (this.updatePriority)
			{
				this.effectivePriority = minPriority;
				for (final ThreadState threadStateObject : this.threadsList)
				{
					//updating values
					int maxValue = Math.max(this.effectivePriority, threadStateObject.getEffectivePriority());
					this.effectivePriority = maxValue;
				}
				this.updatePriority = false;
			}
			return effectivePriority;    //returning effective priority
		}

		public void print()
		{
			Lib.assertTrue(Machine.interrupt().disabled());
			for (final ThreadState threadStateObject : this.threadsList) {
				System.out.println(threadStateObject.getEffectivePriority());
			}
		}

		private void incorrectCashedPriority()
		{
			if (!this.transferPriority) return;

			this.updatePriority = true;

			if (this.resourceHolder != null) {
				resourceHolder.incorrectCashedPriority();
			}
		}


		//declare and initialize appropriate variables
		protected ThreadState resourceHolder = null;
		protected int effectivePriority = priorityMinimum;
		protected boolean updatePriority = false;
		protected final List<ThreadState> threadsList;
		public boolean transferPriority;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState 
	{
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);

			this.availableResources = new LinkedList<PriorityQueue>();
			this.requiredResources = new LinkedList<PriorityQueue>();

		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {

			//if resources is empty
			boolean flag = this.availableResources.isEmpty();
			boolean updatePriority = this.updatePriority;
			if (flag==true) 
			{
				return this.getPriority();
			}
			//if priority chage is occur
			else if (updatePriority==true) 
			{
				this.effectivePriority = this.getPriority();
				for (final PriorityQueue priorityQueueObject : this.availableResources)
				{
					int maxPrior = Math.max(this.effectivePriority, priorityQueueObject.getEffectivePriority());
					this.effectivePriority = maxPrior;
				}
				this.updatePriority = false;
			}
			return this.effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param priority the new priority.
		 */
        // public void to setPriority in place
		public void setPriority(int priority)
		{
			if (this.priority == priority)
				return;
			this.priority = priority;
			if (requiredResources != null)
			{
				for (final PriorityQueue priorityQueueObject : requiredResources) 
				{
					priorityQueueObject.incorrectCashedPriority();
				}
			}
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param waitQueue the queue that the associated thread is
		 *                  now waiting on.
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			this.requiredResources.add(waitQueue);
			this.availableResources.remove(waitQueue);
			waitQueue.incorrectCashedPriority();
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
        // add to the pqObject, and remove the pqObject
		public void acquire(PriorityQueue pqObject)
		{
			this.availableResources.add(pqObject);
			this.requiredResources.remove(pqObject);
			this.incorrectCashedPriority();
		}

		//release function
		public void release(PriorityQueue  pqObject) 
		{
			this.availableResources.remove(pqObject);// remove the avialbe resource from the pqObject
			this.incorrectCashedPriority();
            // does not have the incorrectCashed Priority
		}

		public KThread getThread() {
			return thread;
		}

		private void incorrectCashedPriority() {
			if (this.updatePriority) return;
			this.updatePriority = true;
			for (final PriorityQueue pq : this.requiredResources) {
				pq.incorrectCashedPriority();
			}
		}

		//declare class 'protected' variables
		protected KThread thread;		
		protected int priority;
		protected final List<PriorityQueue> availableResources;
		protected final List<PriorityQueue> requiredResources;
		protected boolean updatePriority = false;
		protected int effectivePriority = priorityMinimum;
	}
}
