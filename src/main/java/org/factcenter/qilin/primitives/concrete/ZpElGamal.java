package org.factcenter.qilin.primitives.concrete;

import org.factcenter.qilin.primitives.generic.ElGamal;
import org.factcenter.qilin.util.IntegerUtils;

import java.math.BigInteger;
import java.util.Random;


/**
 * A modular-arithmetic implementation of {@link ElGamal}.
 * @author talm
 *
 */
public class ZpElGamal {

	static public class PK extends ElGamal.PK<BigInteger> {
		public PK(Zpsafe grp, BigInteger pk) {
			super(grp, pk);
		}
	}
	
	static public class SK extends ElGamal.SK<BigInteger> {
		public SK(Zpsafe grp, BigInteger sk) {
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
	public static BigInteger generateSecretKey(Zpsafe grp, Random rand) {
		BigInteger sk = IntegerUtils.getRandomInteger(grp.orderUpperBound(), rand);
		return sk;
	}
}
