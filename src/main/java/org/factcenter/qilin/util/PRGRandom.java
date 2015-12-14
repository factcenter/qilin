package org.factcenter.qilin.util;

import org.factcenter.qilin.primitives.PseudorandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.factcenter.qilin.primitives.generic.BlockCipherPRG;

import java.security.SecureRandom;
import java.util.Random;

@SuppressWarnings("serial")
public class PRGRandom extends Random {
	final Logger logger = LoggerFactory.getLogger(getClass());
	/**
	 * Fast PRG for generating many random bytes (turns out {@link SecureRandom} uses
	 * up significant time resources).
	 */
	PseudorandomGenerator prg;

	/**
	 * Create a new PRG-based random number generator. 
	 * This will be fast for generating long sequences of random bytes.
	 * If seed is not null, it will be used to seed the generator
	 * instead of a securely generated random seed (the output of the PRG is
	 * deterministically computed from the seed in this case).
	 * @param seed
	 */
	public PRGRandom(byte[] seed) {
        prg = new BlockCipherPRG();

        if (seed == null) {
            prg.generateKey();
        } else {
            prg.setKey(seed);
        }
    }
	
	/**
	 * Constructs a new PRG-based random number generator with a randomly generated seed.
	 * (equivalent to calling {@link #PRGRandom(byte[])} with a null parameter). 
	 */
	public PRGRandom() {
		this(null);
	}

	@Override
	protected int next(int bits) {
		byte[] nextBits = new byte[4];
		prg.getPRGBytes(nextBits, 0, 3);

		return nextBits[0] | ((nextBits[1] << 8) & 0xff00) | ((nextBits[2] << 16) & 0xff0000) | ((nextBits[3] << 24) & 0xff000000);
	}

	@Override
	public void nextBytes(byte[] bytes) {
		prg.getPRGBytes(bytes, 0, bytes.length);
	}

	/**
	 * Identical to {@link #nextBytes(byte[])}, except allows writing into a subset of the array 
	 * @param bytes output array
	 * @param offset offset at which to start writing
	 * @param len number of bytes to write.
	 */
	public void nextBytes(byte[] bytes, int offset, int len) {
		prg.getPRGBytes(bytes, offset, len);
	}
}
