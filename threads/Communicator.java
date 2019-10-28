package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    private Lock master;
    private int waitingListeners;
    private int waitingSpeakers;
    private int message;
    private int word;
    private boolean messageReceived;
    private Condition2 activeSpeaker;
    private Condition2 activeListener;

    public Communicator() {
        master = new Lock();
        waitingListeners = 0;
        waitingSpeakers = 0;
        messageReceived = false;
        message = 0;
	word = 0;
        activeSpeaker = new Condition2(master);
        activeListener = new Condition2(master);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        master.acquire();
        waitingSpeakers++;
        while(waitingSpeakers > 0 || !messageReceived){
            activeSpeaker.sleep();
        }
        message = word;
        messageReceived = true;
        activeListener.wakeAll();
        waitingSpeakers--;
        master.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        master.acquire();
        waitingListeners++;
        while(!messageReceived){
            activeSpeaker.wakeAll();
            activeListener.sleep();
        }
	int temp = this.word;
        waitingListeners--;
        master.release();
        return temp;
    }
}
