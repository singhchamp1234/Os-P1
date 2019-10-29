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
    private boolean messageReadyToListen;
    private Condition2 activeSpeaker;
    private Condition2 activeListener;
    private Condition2 speakerQueue;
    private Condition2 listenerQueue;

    public Communicator() {
        master = new Lock();
        waitingListeners = 0;
        waitingSpeakers = 0;
        messageReadyToListen = false;
        message = 0;
        activeSpeaker = new Condition2(master);
        activeListener = new Condition2(master);
        speakerQueue = new Condition2(master);
        listenerQueue = new Condition2(master);
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
        // ensure only 1 waiting speaker is sleeping as active speaker while the rest sleep in queue
        while (waitingSpeakers == 1) {
            speakerQueue.sleep();
        }
        waitingSpeakers++;
        // wait for a listener to pair with
        while (waitingListeners == 0 || messageReadyToListen == true) {
            activeSpeaker.sleep();
        }
        waitingSpeakers--;
        message = word;
        messageReadyToListen = true;
        // wake up pair listener
        activeListener.wake();

        // wake up next speaker in queue
        speakerQueue.wake();

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
        // ensure only 1 waiting listener is sleeping as active listener while the rest sleep in queue
        while (waitingListeners == 1) {
            listenerQueue.sleep();
        }
        waitingListeners++;
        // wait for a speaker to pair with
        while (messageReadyToListen == false) {
            activeSpeaker.wake();
            activeListener.sleep();
        }
        waitingListeners--;
        messageReadyToListen = false;

        // wake up next listener in queue
        listenerQueue.wake();

        master.release();
        return message;
    }
}
