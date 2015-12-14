package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.primitives.NonInteractiveCommitment;
import org.factcenter.qilin.primitives.RandomOracle;
import org.factcenter.qilin.util.ByteEncoder;
import org.factcenter.qilin.util.EncodingUtils;

import java.util.Arrays;
import java.util.Random;


/**
 * Commitment using random oracle. Commitment and randomness are both byte arrays.
 * @author talm
 *
 * @param <E> Type of elements to commit to.
 */
public class RandomOracleCommitment<E> implements
		NonInteractiveCommitment.Committer<byte[], E, byte[]>,
			NonInteractiveCommitment.Verifier<byte[], E, byte[]> {

	RandomOracle H;
	ByteEncoder<E> encoder;
	int hashlen;
	
	public RandomOracleCommitment(RandomOracle H, int hashlen, ByteEncoder<E> encoder) {
		this.H = H;
		this.hashlen = hashlen;
		this.encoder = encoder;
	}
	
	/**
	 * Commit to an element by encoding it as a byte string, then
	 * concatenating the randomness and hashing.
	 */
	@Override
	public byte[] commit(E element, byte[] randomness) {
		byte[] encodedElement = encoder.encode(element);
		byte[] encoded = new byte[8 + encodedElement.length + randomness.length];
		int pos = 0;
		pos += EncodingUtils.encode(encodedElement, encoded, pos);
		EncodingUtils.encode(randomness, encoded, pos);
		
		return H.hash(encoded, hashlen);
	}

	@Override
	public boolean verifyCommitment(byte[] commitment) {
		return true;
	}

	@Override
	public boolean verifyOpening(byte[] commitment, E element,
			byte[] randomness) {
		return Arrays.equals(commit(element, randomness), commitment);
	}

	@Override
	public byte[] getRandom(Random rand) {
		byte[] r = new byte[hashlen];
		rand.nextBytes(r);
		return r;
	}
}
