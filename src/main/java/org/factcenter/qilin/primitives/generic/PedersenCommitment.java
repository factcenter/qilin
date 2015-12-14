package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.primitives.Cipher;
import org.factcenter.qilin.primitives.CyclicGroup;
import org.factcenter.qilin.primitives.Homomorphic;
import org.factcenter.qilin.primitives.NonInteractiveCommitment;
import org.factcenter.qilin.util.IntegerUtils;

import java.math.BigInteger;
import java.util.Random;

/**
 * An implementation of Pedersen Commitment over a generic group.
 * Pedersen commitment is {@link Homomorphic} and perfectly hiding. It is binding as long as
 * the discrete-log problem in the underlying group is hard.
 *    
 * @author talm
 *
 * @param <G> the group element type. This type is used as the commitment type.
 */
public class PedersenCommitment<G> implements NonInteractiveCommitment.Committer<G,BigInteger,BigInteger>,
									NonInteractiveCommitment.Verifier<G,BigInteger,BigInteger>,
		Cipher.PK<G,BigInteger,BigInteger>,
									Homomorphic<G,BigInteger,BigInteger> {
	
	protected CyclicGroup<G> grp;
	
	/**
	 * First Generator for the group G.
	 */
	transient protected G g;
		
	/**
	 * Second Generator for the group G.
	 * 
	 * Breaking binding property is equivalent 
	 * to computing discrete log of g in base h. 
	 */
	protected G h;
	
	public PedersenCommitment(CyclicGroup<G> grp, G h) {
		this.grp = grp;
		this.g = grp.getGenerator();
		this.h = h;
	}
	
	public CyclicGroup<G> getGroup() {
		return grp;
	}
	
	public G getG() {
		return g;
	}
	
	public G getH() {
		return h;
	}
	
	/**
	 * Compute a Pedersen commitment: msg g + rnd h 
	 * @param msg The message to be committed
	 * @param rnd a random value masking the commitment
	 * @return the commitment (a group element)
	 */
	public G commit(BigInteger msg, BigInteger rnd) {
		return grp.add(grp.multiply(g,msg), grp.multiply(h,rnd));
	}

	@Override
	public BigInteger getRandom(Random rand) {
		return IntegerUtils.getRandomInteger(grp.orderUpperBound(), rand);
	}
	
	public final G encrypt(BigInteger msg, BigInteger rnd) {
		return commit(msg, rnd);
	}
	
	/**
	 * Verify a commitment opening by recomputing commitment and comparing
	 *  
	 * @param commitment
	 * @param msg
	 * @param rnd
	 * @return true iff (msg,rnd) is a valid opening of commitment.
	 */
	public boolean verify(G commitment, BigInteger msg, BigInteger rnd) {
		return commitment.equals(commit(msg,rnd));
	}

	@Override
	public G multiply(G commitment, BigInteger scalar) {
		return grp.multiply(commitment, scalar);
	}

	@Override
	public G add(G commitment1, G commitment2) {
		return grp.add(commitment1, commitment2);
	}

	@Override
	public BigInteger rndadd(BigInteger msg1, BigInteger rnd1, BigInteger msg2, BigInteger rnd2) {
		return rnd1.add(rnd2);
	}

	@Override
	public BigInteger rndmul(BigInteger msg, BigInteger rnd, BigInteger scalar) {
		return rnd.multiply(scalar).mod(grp.orderUpperBound());
	}

	@Override
	public boolean verifyCommitment(G commitment) {
		return grp.contains(commitment);
	}

	@Override
	public boolean verifyOpening(G commitment, BigInteger element,
			BigInteger randomness) {
		return commit(element, randomness).equals(commitment);
	}

	@Override
	public G negate(G cipher) {
		return grp.negate(cipher);
	}

	@Override
	public BigInteger rndnegate(BigInteger msg, BigInteger rnd) {
		return grp.orderUpperBound().subtract(rnd);
	}
}
