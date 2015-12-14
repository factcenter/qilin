package org.factcenter.qilin.protocols;

import java.io.IOException;

/**
 * This is a version of a commitment Proof-of-Knowledge in which there exists
 * a <i>trapdoor</i> for the prover. 
 * Using the secret trapdoor key, the prover can generate `fake' proofs that
 * will nevertheless be accepted by the verifier.
 * 
 * @see CommitmentPoK
 *
 */
public interface TrapdoorCommitmentPoK extends CommitmentPoK {
	/**
	 * Represents a prover holding the secret trapdoor key for the proof-of-knowledge. 
	 *
	 * @param <C> Commitment type.
	 * @param <P> Plaintext type.
	 * @param <R> Randomness type.
	 */
	public interface TrapdoorProver<C, P, R> extends Prover<C, P, R> {
		/**
		 * Use the trapdoor to generate a fake proof that the prover 
		 * knows an opening of com1 to value plain1
		 *  
		 * @param com1
		 * @param plain1
		 * 		 
		 */
		public void proveFakeValue(C com1, P plain1) throws IOException;
	}
	
}
