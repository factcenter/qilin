package org.factcenter.qilin.primitives.concrete;

import org.factcenter.qilin.primitives.generic.ElGamalTest;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;


@RunWith(Enclosed.class)
public class ZpElGamalTest extends ElGamalTest<BigInteger> {
	public final static int[] BITS = {8, 64, 256}; 
	
	public ZpElGamalTest(Random rand, Zpsafe grp, ZpElGamal.SK sk,
						 ZpElGamal.PK pk) {
		super(rand, grp, sk, pk);
	}
	
	public static Collection<Object[]> getTestParameters() {
		Random rand = new Random(1);
		List<Object[]> params = new ArrayList<Object[]>(BITS.length);
		for (int bits : BITS) {
			Zpsafe grp = new Zpsafe(Zpsafe.randomSafePrime(bits, 50, rand));
			BigInteger skval = ZpElGamal.generateSecretKey(grp, rand);
			ZpElGamal.SK sk = new ZpElGamal.SK(grp, skval);
			ZpElGamal.PK pk = new ZpElGamal.PK(grp, sk.getPK());
			Object[] param = {new ZpElGamalTest(rand, grp, sk, pk)};
			params.add(param);
		}
		return params;
	}
	
	@RunWith(Parameterized.class)
	public static class Cipher extends ElGamalTest.Cipher<BigInteger> {

		public Cipher(ElGamalTest<BigInteger> globals) {
			super(globals);
		}
		
		@Parameters
		public static Collection<Object[]> getTestParameters() {
			return ZpElGamalTest.getTestParameters();
		}
	}
	

	@RunWith(Parameterized.class)
	public static class Homomorphic extends ElGamalTest.Homomorphic<BigInteger> {

		public Homomorphic(ElGamalTest<BigInteger> globals) {
			super(globals);
		}
		
		@Parameters
		public static Collection<Object[]> getTestParameters() {
			return ZpElGamalTest.getTestParameters();
		}
	}

}
