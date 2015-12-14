package org.factcenter.qilin.util;

/**
 * Handle encoding and decoding elements to/from byte arrays.
 * @author talm
 *
 * @param <T> type of elements
 */
public interface ByteEncoder<T> {
	/**
	 * Encode an element of type T as a byte array.
	 * The encoding should be unique.
	 *
	 */
	public byte[] encode(T input);

	/**
	 * Decode an element of type T from a byte array.
	 * The input must be a possible output of {@link #encode(Object)}.
	 * Running {@link #encode(Object)} on the return value will 
	 * give back the input.
	 *
	 */
	public T decode(byte[] input);
	
	/**
	 * Return the minimum length required to describe
	 * an element of type T (for dense decoding).
	 * 
	 * @return the minimum length in bytes.
	 */
	int getMinLength();

	/**
	 * Decode an element of type T from a byte array.
	 * The input can be an arbitrary byte array of length
	 * length at least {@link #getMinLength()}.
	 * Running denseDecode on an array chosen uniformly at random
	 * should result in an output indistinguishable from uniform.
	 *  
	 * Note that the input to this method may give "secret" side
	 * information about the returned element (e.g., the discrete
	 * log with respect to some base). 
	 *
	 */
	public T denseDecode(byte[] input);
}
