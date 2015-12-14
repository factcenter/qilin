package org.factcenter.qilin.primitives.concrete;

import org.factcenter.qilin.primitives.generic.TrapdoorPedersenCommitment;

import java.math.BigInteger;

/**
 * A modular-arithmetic implementation of {@link TrapdoorPedersenCommitment}.
 * @author talm
 *
 */
public class ZpTrapdoorPedersen extends TrapdoorPedersenCommitment<BigInteger> {
	protected ZpTrapdoorPedersen(Zpsafe grp, BigInteger sk) {
		super(grp, sk);
	}
}
