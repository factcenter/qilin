package org.factcenter.qilin.protocols.concrete;

import org.factcenter.qilin.primitives.RandomOracle;
import org.factcenter.qilin.primitives.concrete.Zpsafe;
import org.factcenter.qilin.protocols.generic.NaorPinkasOT;

import java.math.BigInteger;

/**
 * A modular-arithmetic implementation of Naor-Pinkas OT.
 * 
 * @author talm
 * @see ECNaorPinkasOT
  */
public class ZpNaorPinkasOT extends NaorPinkasOT<BigInteger> {
	public ZpNaorPinkasOT(RandomOracle H, Zpsafe grp) {
		super(grp, H, grp);
	}
}
