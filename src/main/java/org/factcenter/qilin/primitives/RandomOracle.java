package org.factcenter.qilin.primitives;

/**
 * Represents a <i>Random Oracle</i>. The Random Oracle is a theoretical construct that is an idealization
 * of a cryptographic hash-function (it is equivalent to an <i>ideal cipher</i>). Although 
 * it is impossible to actually implement a Random Oracle, it is much easier to analyze 
 * in theoretical constructions, and consequently is used in many protocols. 
 * In most cases, we do not know how to break protocols that replace the Random Oracle
 * with a secure cryptographic hash function (assuming the protocol is secure in the Random Oracle model).
 * 
 * This interface Random Oracle is easy to use when both the position at which the random oracle is queried
 * and the required output length are known in advance.
 * 
 * @see StreamingRandomOracle
 *
 */
public interface RandomOracle {
	/**
	 * Hash an arbitrary length input to an arbitrary length output. 
	 * @param input the input to be hashed
	 * @param outlen the number of bytes to return.
	 * @return the hashed input (an array of length outlen). If this method is called
	 *   on the same input for two different output lengths, the output on the shorter
	 *   length will be a prefix of the output on the longer. 
	 */
	public byte[] hash(byte[] input, int outlen);
	
	/**
	 * Get the "native" length returned by a call to
	 * the digest function. Running {@link #hash(byte[], int)} 
	 * with this as the length value will be most efficient.
	 * @return the native length (in bytes)
	 */
	public int getNativeLength();
}
