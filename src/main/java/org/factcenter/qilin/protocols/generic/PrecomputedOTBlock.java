package org.factcenter.qilin.protocols.generic;

/**
 * Created by talm on 8/8/14.
 */
public interface PrecomputedOTBlock<E> {
    public int getNumOTs();

    public E remove(int numOts);
}
