package org.factcenter.qilin.primitives;

import java.util.Random;

/**
 * This interface represents generic public-key encryption (or commitment). 
 * It is actually only a namespace; the methods are defined in the {@link Cipher.PK} and {@link Cipher.SK} sub-interfaces.  
 * @author talm
 *
 */
public interface Cipher {

	/**
	 * Represents the public key of a cipher. It allows encryption but not necessarily decryption.
	 * @author talm
	 *
	 * @param <C> Ciphertext space
	 * @param <P> Plaintext space
	 * @param <R> Randomizer space
	 */
	public interface PK<C,P,R> {
		/**
		 * Get a randomizer suitable for use with {@link #encrypt(Object, Object)}
		 * 
		 * @param rand A rand  
		 * @return a uniformly chosen value from the Randomizer space.
		 */
		public R getRandom(Random rand);
		
		/**
		 * Encrypt the plaintext using randomness to randomize
		 * @param plaintext
		 * @param randomness
		 * @return a ciphertext
		 */
		public C encrypt(P plaintext, R randomness);
	}

	/**
	 * Represents the secret key of a cipher. It extends {@link PK} and allows both encryption and decryption.
	 * @author talm
	 *
	 * @param <C> Ciphertext space
	 * @param <P> Plaintext space
	 * @param <R> Randomizer space
	 */

	public interface SK<C,P,R> extends PK<C,P,R> {
		/**
		 * Decrypt a ciphertext. Note that no 
		 * @param ciphertext
		 * @return the decrypted ciphertext (null if there was an error).
		 */
		public P decrypt(C ciphertext);
	}
}
