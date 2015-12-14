package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.primitives.CyclicGroup;
import org.factcenter.qilin.primitives.TrapdoorNonInteractiveCommitment;
import org.factcenter.qilin.util.IntegerUtils;

import java.math.BigInteger;
import java.util.Random;


/**
 * An implementation of {@link PedersenCommitment} over a generic group
 * with the addition of a trapdoor for equivocation. 
 * @author talm
 *
 * @param <G> the underlying group element type.
 */
public class TrapdoorPedersenCommitment<G> extends PedersenCommitment<G> 
implements TrapdoorNonInteractiveCommitment.EquivocatingCommitter<G, BigInteger, BigInteger> {
	/**
	 * Secret key for equivocation.
	 * The secret key is the discrete log of {@link #h} to base
	 * {@link #g}.
	 */
	BigInteger sk;
	
	/**
	 * Inverse of the secret key modulo the group order. 
	 */
	BigInteger skInv;
	
	/**
	 * Construct a new trapdoor pedersen commitment from a given group, 
	 * generator and secret key. 
	 * The secret key must be relatively prime to the group order (otherwise
	 * the constructor will throw an {@link ArithmeticException}.  
	 * @param grp
	 * @param sk
	 */
	public TrapdoorPedersenCommitment(CyclicGroup<G> grp, BigInteger sk) {
		super(grp, grp.multiply(grp.getGenerator(), sk));
		skInv = sk.modInverse(grp.orderUpperBound());
	}
	
	@Override
	public BigInteger equivocate(BigInteger oldvalue, BigInteger oldrandom, BigInteger newvalue) {
		// Compute the new randomness: (oldvalue - newvalue) / sk + oldrandom.
		// Since h=sk*g, we have:
		//  commit(newvalue, newrandom)	= newvalue * g + newrandom * h 
		//								= newvalue * g + (oldvalue - newvalue) / sk + oldrandom) * sk * g
		//								= (newvalue + oldvalue - newvalue + oldrandom * sk) * g 
		//								= oldvalue * g + oldrandom * h
		BigInteger newRandom = oldvalue.subtract(newvalue).multiply(skInv).add(oldrandom);
		return newRandom.mod(grp.orderUpperBound());
	}
	
	/**
	 * Generate a new private key.
	 * @param grp
	 * @param rand
	 * @return the private key.
	 */
	public static <G> BigInteger generateKey(CyclicGroup<G> grp, Random rand) {
		BigInteger groupOrder = grp.orderUpperBound();
		BigInteger sk;
		do {
			sk = IntegerUtils.getRandomInteger(groupOrder, rand);
		} while (!sk.gcd(groupOrder).equals(BigInteger.ONE));
		return sk;
	}
}
