package org.factcenter.qilin.util;

import java.math.BigInteger;
import java.util.Random;

/**
 * Utilities for dealing with big integers.
 * @author talm
 *
 */
public class IntegerUtils {
	/**
	 * Return a uniformly chosen integer between 0 and n-1.
	 */
	public static BigInteger getRandomInteger(BigInteger n, Random rand) {
		BigInteger r;
		int maxbits = n.bitLength();
		do {
			r = new BigInteger(maxbits, rand);
		} while (r.compareTo(n) >= 0);
		return r;
	}
}
