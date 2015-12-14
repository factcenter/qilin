package org.factcenter.qilin.primitives.concrete;

import org.factcenter.qilin.primitives.RandomOracle;
import org.factcenter.qilin.primitives.StreamingRandomOracle;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;


/**
 * "random oracle" implementation using plain digest
 * (Note: this may not be very secure).
 * @author talm
 *
 */
public class DigestOracle implements RandomOracle, StreamingRandomOracle {
	final static String DEFAULT_ALGORITHM = "SHA-256";
	
	MessageDigest md;
	

	public DigestOracle(String algorithm) throws NoSuchAlgorithmException {
		md = MessageDigest.getInstance(algorithm);
	}
	
	public DigestOracle() {
		try {
			md = MessageDigest.getInstance(DEFAULT_ALGORITHM);
		} catch (NoSuchAlgorithmException nsa) {
			// We don't want to require try-catch blocks to 
			// check whether the default algorithm is supported.
			throw new RuntimeException(nsa.getMessage());
		}
	}
	
	public byte[] hash(byte[] input, int outlen) {
		//int blocklen = md.getDigestLength();
		
		byte[] out = new byte[outlen];
		
		int done = 0;
		int i = 0;
		
		// Hash blocks with block number appended as an integer in LSB-first order
		do {
			md.reset();
			md.update(input);
			md.update((byte) (i & 0xff));
			md.update((byte) ((i >>> 8) & 0xff));
			md.update((byte) ((i >>> 16) & 0xff));
			md.update((byte) ((i >>> 24) & 0xff));
			++i;
			byte[] digest = md.digest();
			int copylen = Math.min(outlen - done, digest.length);
			System.arraycopy(digest, 0, out, done, copylen);
			done += copylen;
		} while (done < outlen);

		return out;
	
	}

	public Random getRandomStream(byte[] input) {
        return new DigestRandom(md, input);
    }

	/**
	 * Get the "native" length returned by a call to
	 * the digest function. Running {@link #hash(byte[], int)} 
	 * with this as the length value will be most efficient.
	 * @return the length in bytes
	 */
	public int getNativeLength() {
		return md.getDigestLength();
	}

	@Override
	public byte[] digest(int index, int outlen) {
		byte[] retval = new byte[outlen];
		
		try {
			MessageDigest md2 = (MessageDigest) md.clone();
			md.update((byte) (index & 0xff));
			md.update((byte) ((index >>> 8) & 0xff));
			md.update((byte) ((index >>> 16) & 0xff));
			md.update((byte) ((index >>> 24) & 0xff));
			for (int i = 0; i < outlen;) {
				MessageDigest md3 = (MessageDigest) md2.clone();
				md3.update((byte) i);
				byte[] output = md3.digest();
				int copylen = Math.min(outlen - i, output.length);
				System.arraycopy(output, 0, retval, i, copylen);
				i += copylen;
			}
			return retval;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Can't run oracle when clone is not supported!");
		}
	}

	@Override
	public void reset() {
		md.reset();
	}

	@Override
	public void update(byte[] input, int offs, int len) {
		md.update(input, offs, len);
	}
}
