package org.factcenter.qilin.primitives;


import org.factcenter.qilin.util.GlobalTestParams;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.assertTrue;


abstract public class CipherPKTest<C, P, R> implements GlobalTestParams {

	abstract public Cipher.PK<C,P,R> getPK();
	abstract protected Random getRand();

	abstract protected P getRandomPlaintext();
	
	/**
	 * Return an approximation of the number of possible ciphertexts for a 
	 * given plaintext. If this exceeds the size of float, return 
	 * Float.POSITIVE_INFINITY.  
	 */
	abstract protected float getCiphertextOrderApprox();
	
	/**
	 * Test that encrypting the same plaintext multiple times
	 * gives the expected number of different ciphertexts.
	 */
	@Test
	public void testNonEncryptionRandomness() {
		HashSet<C> set = new HashSet<C>();
		
		P plain = getRandomPlaintext();
		float expSize = 0;
		float cOrder = getCiphertextOrderApprox(); 
		for (int i = 0; i < CONFIDENCE; ++i) {
			expSize += 1 - set.size()/cOrder;
			R rnd = getPK().getRandom(getRand());
			C ciph = getPK().encrypt(plain, rnd);
			set.add(ciph);
		}
		
		assertTrue("Too few ciphertexts for multiple encryptions (expected "+ 
				(int)expSize+", received "+set.size()+")",
				set.size() > expSize / 4);
	}
	
}
