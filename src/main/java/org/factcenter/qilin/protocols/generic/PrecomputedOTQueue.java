package org.factcenter.qilin.protocols.generic;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by talm on 8/8/14.
 */
public class PrecomputedOTQueue<E extends PrecomputedOTBlock<E>> {
    /**
     * An interface used to pass a callback object (allowing the client to request additional precomputed OTs).
     * @author talm
     *
     */
    public interface RequestAdditionalOTCallback {
        public void makeRequest();
    }

    RequestAdditionalOTCallback needOTCallback;

    public void setNeedOTCallback(RequestAdditionalOTCallback needOTCallback) {
        this.needOTCallback = needOTCallback;
    }

    /**
     * Data for the last block OT
     * The block contains m OTs of length k.
     */
    E curOTs;

    int lowWaterMark;

    /**
     * A queue of precomputed OT blocks.
     * OT blocks are computed asynchronously, and added to the queue when ready.
     * Once the current OT block ({@link #curOTs}) runs out,
     *
     */
    private Queue<E> precomputedOTs;

    /**
     * Total number of available precomputed choice OTs.
     */
    private int availableOTs;

    public PrecomputedOTQueue(int lowWaterMark) {
        this.lowWaterMark = lowWaterMark;
        precomputedOTs = new LinkedList<E>();
    }


    public synchronized  int getAvailableOTs() {
        return availableOTs;
    }

    public PrecomputedOTQueue() {
        this(0);
    }

    /**
     * Add a precomputed block of choice OTs to the queue and notify blocked calls. May be safely
     * called from another thread.
     * @param otBlock
     */
    public synchronized void addOTs(E otBlock) {
        precomputedOTs.add(otBlock);
        availableOTs += otBlock.getNumOTs();
        notifyAll();
    }


    public synchronized E getOTs(int numOTs, int reserved) {
        E retval;
        if (numOTs <= 0)
            return null;

        if (availableOTs <= reserved && needOTCallback != null)
            needOTCallback.makeRequest();

        // If we have reserved OTs but not enough available, wait.
        while (availableOTs <= reserved) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Ignore for now.
            }
        }
        if (curOTs == null || curOTs.getNumOTs() <= 0) {
            curOTs = precomputedOTs.remove();
        }

        int otsUsed = curOTs.getNumOTs();
        if (otsUsed > numOTs)
            otsUsed = numOTs;

        retval = curOTs.remove(otsUsed);
        availableOTs -= otsUsed;
        if ((otsUsed - reserved) < lowWaterMark && needOTCallback != null) {
            needOTCallback.makeRequest();
        }
        return retval;
    }
}
