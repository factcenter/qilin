package org.factcenter.qilin.primitives;

import java.math.BigInteger;

/**
 * Represents a Homomorphic cryptosystem or commitment scheme. 
 * A cryptosystem that implements this interface allows adding ciphertexts and multiplication
 * of ciphertexts by a scalar.
 * 
 * @author talm
 *
 * @param <C> Ciphertext type
 * @param <P> Plaintext type
 * @param <R> Randomness type
 */
public interface Homomorphic<C,P,R> {
	
	/**
	 * Add two ciphertexts together
	 * @param cipher1
	 * @param cipher2
	 * @return a ciphertext that encodes the sum of the two
	 */
	public C add(C cipher1, C cipher2);
	
	/**
	 * Return a ciphertext that encodes the negation of plaintext
	 * encoded by the input
	 * @param cipher
	 * @return c such that if cipher=encrypt(msg) then c=encrypt(-msg)
	 */
	public C negate(C cipher); 
	
	/**
	 * Compute the corresponding randomness for the ciphertext created by 
	 * negating a ciphertext  
	 * @param msg message to negate
	 * @param rnd randomness
	 * @return r such that negate(cipher(msg,rnd))=cipher(negate(msg),r)
	 */
	public R rndnegate(P msg, R rnd); 
	
	
	/**
	 * Compute the corresponding randomness for the ciphertext created by adding 
	 * two ciphertexts  
	 * @param msg1
	 * @param rnd1
	 * @param msg2
	 * @param rnd2
	 * @return r such that add(cipher(msg1,rnd1),cipher(msg2,rnd2))=cipher(msg1.add(msg2),rnd2)
	 */
	public R rndadd(P msg1, R rnd1, P msg2, R rnd2);
	
	/**
	 * Multiply a ciphertext by a scalar
	 * @param cipher
	 * @param scalar
	 * @return c such that if cipher=encrypt(msg) then c=encrypt(msg * scalar) 
	 */
	public C multiply(C cipher, BigInteger scalar);

	/**
	 * Compute the corresponding randomness for the ciphertext created by multiplying 
	 * a ciphertext by an integer  
	 * @param msg
	 * @param rnd
	 * @param scalar
	 * @return r such that multiply(cipher(msg,rnd),scalar)=cipher(msg1.multiply(scalar),rnd2)
	 */
	public R rndmul(P msg, R rnd, BigInteger scalar);
}
