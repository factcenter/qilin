package org.factcenter.qilin.primitives.concrete;

import org.factcenter.qilin.primitives.CyclicGroup;
import org.factcenter.qilin.util.IntegerUtils;

import java.math.BigInteger;
import java.util.Random;



/**
 * The order q subgroup of the multiplicative group of integers mod p, where p=2q+1
 * and both p and q are prime.
 * 
 * @author talm
 *
 */
public class Zpsafe extends Zpstar implements CyclicGroup<BigInteger> {
	
	/**
	 * Order of the group
	 */
	BigInteger q;
	
	/**
	 *  Generator for the group.
	 */
	BigInteger g;

	/**
	 * Return a random safe prime suitable for use with this class
	 * @param bits
	 */
	public static BigInteger randomSafePrime(int bits, int certainty, Random rand) {
		BigInteger q;
		BigInteger p;
		
		do {
			q = BigInteger.probablePrime(bits, rand);
			// p = 2q+1
			p = q.shiftLeft(1).setBit(0);
		} while (!p.isProbablePrime(certainty));
		return p;
	}

	/** 
	 * Constructor. 
	 * @param p should be a safe prime (i.e., (p-1)/2 should also be prime).
	 */
	public Zpsafe(BigInteger p) {
		super(p);
		// q=(p-1)/2
		q = p.clearBit(0).shiftRight(1);

		// Since q is prime, every element of Zpstar has order
		// q, 2q or 2. Thus, 4=2^2 must have order 1 or q.
		// Assuming q>2, 4 must have order q.
		g = BigInteger.valueOf(4);
	}
	
	@Override
	public BigInteger sample(Random rand) {
		BigInteger r = IntegerUtils.getRandomInteger(q, rand);
		return g.modPow(r, p);
	}

	/**
	 * Get a random member of the group whose discrete log 
	 * with respect to g is not known.
	 * @param rand
	 * @return a random element in the order q subgroup of Z_p* 
	 */
	public BigInteger randomWithSecretDLOG(Random rand) {
		BigInteger r;
		do {
			r = IntegerUtils.getRandomInteger(p, rand);
			if (r.equals(BigInteger.ONE))
				return r;
			// r=r^2; r has order q, 2q or 2, hence r^2 has order 1 or q 
			r = r.multiply(r).mod(p);

			// Note that since q is prime, every element in the subgroup
			// of order q is a square, so choosing a random square is
			// equivalent to choosing a random element.
		} while (r.equals(BigInteger.ONE));
		return r;
	}
	
	@Override
	public boolean contains(BigInteger g) {
		// Check that ord(g) | q. Since ord(g) is either 1, 2, q or 2q,
		// this ensures ord(g)==q or ord(g)==1.
		return super.contains(g) && (g.modPow(q, p).equals(BigInteger.ONE));
	}

	@Override
	public BigInteger orderUpperBound() {
		return q;
	}
	
	// TODO: override denseDecode so it will always return a group element.
	@Override
	public BigInteger denseDecode(byte[] input) {
		assert false; // Not yet implemented!
		return BigInteger.ZERO;
	}

	@Override
	public BigInteger getGenerator() {
		return g;
	}
}
