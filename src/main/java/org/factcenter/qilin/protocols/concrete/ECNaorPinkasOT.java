package org.factcenter.qilin.protocols.concrete;

import org.bouncycastle.math.ec.ECPoint;
import org.factcenter.qilin.primitives.RandomOracle;
import org.factcenter.qilin.primitives.concrete.ECGroup;
import org.factcenter.qilin.protocols.generic.NaorPinkasOT;


/**
 * Elliptic-curve implementation of Naor-Pinkas OT.
 * @author talm
 * @see ZpNaorPinkasOT
 *
 */
public class ECNaorPinkasOT extends NaorPinkasOT<ECPoint> {
	public ECNaorPinkasOT(RandomOracle H, ECGroup grp) {
		super(grp, H, grp);
	}
}
