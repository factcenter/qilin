package org.factcenter.qilin.protocols;

import org.factcenter.qilin.primitives.NonInteractiveCommitment;

import java.io.IOException;

/**
 * A Zero-Knowledge Proof-of-Knowledge for a committed value in a non-interactive commitment scheme.
 * This can also be used for proving knowledge of the plaintext of an public-key ciphertext (since 
 * that is also a form of non-interactive commitment).
 * 
 * @see NonInteractiveCommitment
 *
 */
public interface CommitmentPoK {
	/**
	 * Represents the Prover in the Zero-Knowledge Proof-of-Knowledge of a Commitment.
	 *
	 * @param <C> Commitment type.
	 * @param <P> Plaintext type. 
	 * @param <R> Randomness type.
	 */
	public interface Prover<C, P, R> extends ProtocolParty {
		/**
		 * Prove that the prover knows an opening of com1 to value plain1
		 * (rand1 is the randomness used to create com1) 
		 * @param com1
		 * @param plain1
		 * @param rand1
		 */
		public void proveValue(C com1, P plain1, R rand1) throws IOException;
	}

	/**
	 * Represents the Verifier in the Zero-Knowledge Proof-of-Knowledge of a Commitment.
	 *
	 * @param <C> Commitment type.
	 * @param <P> Plaintext type. 
	 * @param <R> Randomness type.
	 */
	public interface Verifier<C, P, R> extends ProtocolParty {
		/**
		 * Verify that the prover knows an opening of com1 to value plain1
		 * @param com1
		 * @param plain1
		 * @return true iff the prover knows an opening of com1 to value plain1
		 */
		public boolean verifyValue(C com1, P plain1) throws IOException;
	}
}


