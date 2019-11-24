package nachos.threads;

import nachos.machine.*;

import java.util.Random;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * LotteryScheduler schedules threads using a lottery.
 *
 * <p>
 * Each thread is associated with number of tickets. Random lottery is held among all the tickets
 * when a thread needs to be dequeued, 
 * The thread with winning ticket is chosen from all waiting threads to be dequeued.
 * <p/>
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lotteryScheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocates a new lottery scheduler.
     */
    public LotteryScheduler() {
    }

    @Override
    public void setPriority(KThread kThread, int priorityValue) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priorityValue <= Integer.MAX_VALUE && priorityValue >= 0);

        getThreadState(kThread).setPriority(priorityValue);
    }

     /**
     * Allocates a new lotteryThreadQueue.
     *
     * @param Priority    <tt>true</tt> if this queue should
     * transfer tickets from waiting threads to the owning thread.
     * @return a new lotteryThreadQueue.
     */
    public ThreadQueue newThreadQueue(boolean priority) {
        return new LotteryPriorityQueue(priority);
    }

    @Override
    protected ThreadState getThreadState(KThread kThread) {
		kThread.schedulingState = (kThread.schedulingState == null) ? 
			new LotteryThreadState(kThread) : kThread.schedulingState;
        return (ThreadState) kThread.schedulingState;
    }

    protected class LotteryPriorityQueue extends PriorityQueue {
        LotteryPriorityQueue(boolean priority) {
            super(priority);
            this.entropy = new Random();
        }
        @Override
        public int getEffectivePriority() {
            if (!this.transferPriority) {
                return 0; //minimum priority
            } else if (this.updatePriority) {
                // Effective priorities recalculation
                this.effectivePriority = 0;
                for (final ThreadState current : this.threadsList) {
                    Lib.assertTrue(current instanceof LotteryThreadState);
                    this.effectivePriority = this.effectivePriority + 
						current.getEffectivePriority();
                }
                this.updatePriority = false;
            }
            return effectivePriority;

        }
        @Override
        public ThreadState pickNextThread() {
            int totalTickets = this.getEffectivePriority();
            int winner = 0;
			if(totalTickets > 0){
				winner = entropy.nextInt(totalTickets)
			}
            for (final ThreadState thread : this.threadsList) {
                Lib.assertTrue(thread instanceof LotteryThreadState);
                winner = winner - thread.getEffectivePriority();
                if (winner <= 0) {
                    return thread;
                }
            }
            return null;
        }
        private final Random entropy;
    }
    protected class LotteryThreadState extends ThreadState {
        public LotteryThreadState(KThread kThread) {
            super(kThread);
        }
        @Override
        public int getEffectivePriority() {
            if (this.availableResources.isEmpty() == true) {
                return this.getPriority();
            } else if (this.updatePriority) {
                this.effectivePriority = this.getPriority();
                for (final PriorityQueue pq : this.availableResources) {
                    Lib.assertTrue(pq instanceof LotteryPriorityQueue);
                    this.effectivePriority = this.effectivePriority +
					pq.getEffectivePriority();
                }
                this.updatePriority = false;
            }
            return this.effectivePriority;
        }
    }
}


