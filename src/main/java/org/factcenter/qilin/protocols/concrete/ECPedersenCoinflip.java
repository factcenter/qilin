package org.factcenter.qilin.protocols.concrete;

import org.bouncycastle.math.ec.ECPoint;
import org.factcenter.qilin.primitives.concrete.ECGroup;
import org.factcenter.qilin.primitives.concrete.Zn;
import org.factcenter.qilin.primitives.generic.PedersenCommitment;
import org.factcenter.qilin.protocols.generic.TrapdoorBlumTwoPartyGroupElementFlip;

import java.math.BigInteger;

/**
 * Elliptic-curve implementation of the {@link TrapdoorBlumTwoPartyGroupElementFlip} using a {@link PedersenCommitment} as the
 * trapdoor commitment scheme.
 * @author talm
 *
 */
public class ECPedersenCoinflip extends TrapdoorBlumTwoPartyGroupElementFlip<BigInteger, ECPoint, BigInteger> {
	public ECPedersenCoinflip(ECGroup grp) {
		this(grp, new Zn(grp.orderUpperBound()));
	}
			
	/**
	 * Private constructor that gets the integer group as a parameter (used
	 * to bypass restriction that super must be first line of constructor).
	 * @param grp
	 * @param plainGroup
	 */
	private ECPedersenCoinflip(ECGroup grp, Zn plainGroup) {
		super(plainGroup, plainGroup, grp, plainGroup);
	}
}
