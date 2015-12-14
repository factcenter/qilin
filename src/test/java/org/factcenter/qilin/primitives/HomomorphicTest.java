package org.factcenter.qilin.primitives;


import org.factcenter.qilin.util.GenericsUtils;
import org.factcenter.qilin.util.GlobalTestParams;
import org.factcenter.qilin.util.IntegerUtils;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.junit.Assert.assertTrue;


abstract public class HomomorphicTest<C,P,R> implements GlobalTestParams {
	abstract protected Homomorphic<C,P,R> getHom();
	abstract protected Group<P> getPlaintextGroup();
	abstract protected Cipher.PK<C,P,R> getCipherPK();
	abstract protected Random getRand();
	
	@Test
	public void testSum() {
		Homomorphic<C,P,R> H = getHom();
		Group<P> G = getPlaintextGroup();
		Cipher.PK<C,P,R> PK = getCipherPK();
		Random rand = getRand();
		
		for (int i = 0; i < CONFIDENCE; ++i) {
			P plain1 = G.sample(rand);
			P plain2 = G.sample(rand);
			R r1 = PK.getRandom(rand);
			R r2 = PK.getRandom(rand);
			
			C c1 = PK.encrypt(plain1, r1);
			C c2 = PK.encrypt(plain2, r2);

			P sum = G.add(plain1, plain2);
			R rSum = H.rndadd(plain1, r1, plain2, r2);
 
			C c3a = PK.encrypt(sum, rSum);
			C c3b = H.add(c1, c2);
			
			assertTrue("Homomorphic sum doesn't work", GenericsUtils.deepEquals(c3a, c3b));
		}
	}
	
	@Test
	public void testNegation() {
		Homomorphic<C,P,R> H = getHom();
		Group<P> G = getPlaintextGroup();
		Cipher.PK<C,P,R> PK = getCipherPK();
		Random rand = getRand();
		
		for (int i = 0; i < CONFIDENCE; ++i) {
			P plain1 = G.sample(rand);
			
			R r1 = PK.getRandom(rand);			
			C c1 = PK.encrypt(plain1, r1);

			C c2a = H.negate(c1);

			P plain2 = G.negate(plain1);
			R r2 = H.rndnegate(plain1, r1);
			C c2b = PK.encrypt(plain2, r2);
			
			assertTrue("Homomorphic negation doesn't work",
					GenericsUtils.deepEquals(c2b, c2a));
		}
	}

	
	@Test
	public void testMultiply() {
		Homomorphic<C,P,R> H = getHom();
		Group<P> G = getPlaintextGroup();
		Cipher.PK<C,P,R> PK = getCipherPK();
		Random rand = getRand();
		
		for (int i = 0; i < CONFIDENCE; ++i) {
			P plain = G.sample(rand);
			R r = PK.getRandom(rand);
			BigInteger n = IntegerUtils.getRandomInteger(G.orderUpperBound(), rand);
			
			C c1 = PK.encrypt(plain, r);
			
			C c2a = PK.encrypt(G.multiply(plain, n), H.rndmul(plain, r, n));
			C c2b = H.multiply(c1, n);
			
			assertTrue("Homomorphic product doesn't work", GenericsUtils.deepEquals(c2a, c2b));
		}
	}

}
