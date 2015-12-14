package org.factcenter.qilin.primitives.concrete;

import org.bouncycastle.math.ec.ECPoint;
import org.factcenter.qilin.primitives.generic.ElGamal;
import org.factcenter.qilin.util.IntegerUtils;

import java.math.BigInteger;
import java.util.Random;

/**
 * Elliptic-curve implementation of {@link ElGamal}
 * @author talm
 *
 */
public class ECElGamal {
	static public class PK extends ElGamal.PK<ECPoint> {
		public PK(ECGroup grp, ECPoint pk) {
			super(grp, pk);
		}
	}
	
	static public class SK extends ElGamal.SK<ECPoint> {
		public SK(ECGroup grp, BigInteger sk) {
			super(grp, sk);
		}
	}

	/**
	 * Generate a new private key.
	 * To get the corresponding public key, create an instance of the SK class and use getPK().
	 * @param grp
	 * @param rand
	 * @return a pair containing the private and public keys. pair.a is the private key, and pair.b the public key.
	 */
	public static BigInteger generateSecretKey(ECGroup grp, Random rand) {
		BigInteger sk = IntegerUtils.getRandomInteger(grp.curveParams.getN(), rand);
		return sk;
	}
}
