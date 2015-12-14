package org.factcenter.qilin.primitives;

import java.util.Random;

/**
 * Represents a non-interactive commitment scheme. In a commitment scheme there are two
 * parties, a <i>Committer</i> and a <i>Verifier</i>. These are represented by the sub-interfaces
 * {@link Committer} and {@link Verifier}. 
 * 
 * @author talm
 *
 */
public interface NonInteractiveCommitment {
	/**
	 * Represents the Committer in a non-interactive commitment scheme.
	 *
	 * @param <C> Commitment type
	 * @param <E> Element type
	 * @param <R> Randomness type
	 */
	public interface Committer<C, E, R> {
		/**
		 * Commit to an element.
		 * @param element
		 * @return the new commitment. The randomness is used as a proof for opening.
		 */
		public C commit(E element, R randomness);

		/**
		 * Get a random value suitable for use with the 
		 * {@link #commit(Object, Object)} method.
		 * @param rand
		 * @return a new random value.
		 */
		public R getRandom(Random rand);
	}

	/**
	 * Represents the Verifier in a non-interactive commitment scheme.
	 *
	 * @param <C> Commitment type
	 * @param <E> Element type
	 * @param <R> Randomness type
	 */
	public interface Verifier<C, E, R> {
		/**
		 * Verify that a commitment is valid
		 * @param commitment
		 * @return true iff the commitment is valid.
		 */
		public boolean verifyCommitment(C commitment);
		
		/**
		 * Verify that the commitment was correctly
		 * opened to a specific value.
		 * @param commitment the commitment value.
		 * @param element the element claimed to the opening of the commitment.
		 * @param randomness the randomness used to generate the commitment.
		 * @return true iff verification passed.
		 */
		public boolean verifyOpening(C commitment, E element, R randomness);
		
	}
}
