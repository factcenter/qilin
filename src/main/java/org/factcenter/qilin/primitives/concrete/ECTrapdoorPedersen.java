package org.factcenter.qilin.primitives.concrete;

import org.bouncycastle.math.ec.ECPoint;
import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.primitives.generic.TrapdoorPedersenCommitment;

import java.math.BigInteger;

/**
 * Elliptic-curve implementation of {@link TrapdoorPedersenCommitment}.
 * @author talm
 *
 */
public class ECTrapdoorPedersen extends TrapdoorPedersenCommitment<ECPoint> {
	public ECTrapdoorPedersen(ECGroup grp, BigInteger sk) {
		super(grp, sk);
	}
	
	/**
	 * Allow typesafe use of the group by returning an {@link ECGroup} rather than
	 * a generic {@link Group}.
	 */
	@Override
	public ECGroup getGroup() {
		return (ECGroup) grp;
	}
}
