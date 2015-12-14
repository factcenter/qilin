package org.factcenter.qilin.primitives;

import java.math.BigInteger;
import java.util.Random;

/**
 * Represents an algebraic Group with elements of a generic type.
 * The group is defined generically so that pre-existing types can be used as the element (e.g., {@link BigInteger}).
 * For our purposes, we require every group to be sampleable, hence this interface extends {@link Sampler}. 
 * @author talm
 *
 * @param <G> the group element type 
 */
public interface Group<G> extends Sampler<G> {
	
	/**
	 * Multiply an element of the group by an integer (equivalent to repeated adding)
	 * @param g a group element
	 * @param scalar the integer to multiply by
	 * @return g * scalar
	 */
	public G multiply(G g, BigInteger scalar);
	
	
	/**
	 * Add an element of the group to another element
	 * @param el first summand
	 * @param el2 second summand
	 * @return el + el2
	 */
	public G add(G el, G el2);

	/**
	 * Return the negation of a group element
	 * @param g
	 * @return -g
	 */
	public G negate(G g); 
	
	/**
	 * Return the neutral element in the group with respect to addition.
	 */
	public G zero();
	
	/**
	 * 
	 * @return a random group element
	 */
	public G sample(Random rand);
	
	/**
	 * Check that an element is in the group.
	 * 
	 * @param g
	 * @return true iff the element g is in the group
	 */
	public boolean contains(G g);
	
	/**
	 * Get a bound on the order of the group. The actual order of the group MUST divide this bound
	 * (useful for choosing a random element of the group
	 * with a known discrete log)
	 * @return an upper bound on the order of the group
	 */
	public BigInteger orderUpperBound();

	/**
	 * Encode a message as a group element.
	 * This encoding must be injective and have an efficient decoder (see {@link #injectiveDecode(Object)}).
     * The encoding may be probabilistic (in which case it will use the supplied randomness).
     * Note that the encoding may not be bijective -- not all group elements have a valid decoding.
	 * @param msg The message to be encoded. The message will be encoded as a {@link #getInjectiveEncodeMsgLength()}}-byte
     *            message, if shorter it will be zero-padded at end, if longer truncated.
	 * @return The encoded message.
     * @throws UnsupportedOperationException if the group doesn't support the encoding operation
     *      or if the message is too long.
	 */
	public G injectiveEncode(byte[] msg, Random rand) throws UnsupportedOperationException;

    /**
     * Decode a message encoded using {@link #injectiveEncode(byte[], Random)}.
     * @param encodedMsg
     * @return
     * @throws UnsupportedOperationException
     */
	public byte[] injectiveDecode(G encodedMsg) throws UnsupportedOperationException;

    /**
     *
     * @return the length (in bytes) of a message that can be encoded using the
     * {@link #injectiveEncode(byte[], Random)} method. Shorter messages will be zero-padded at
	 * the end, longer messages will be truncated.
     */
    public int getInjectiveEncodeMsgLength()  throws UnsupportedOperationException;;
}
