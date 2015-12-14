package org.factcenter.qilin.primitives.concrete;

import org.bouncycastle.math.ec.ECPoint;
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
public class ECElGamalTest extends ElGamalTest<ECPoint> {

	public ECElGamalTest(Random rand, ECGroup grp, ECElGamal.SK sk,
						 ECElGamal.PK pk) {
		super(rand, grp, sk, pk);
	}
	
	public static Collection<Object[]> getTestParameters() {
		Random rand = new Random(1);
		List<ECGroup> testGroups = ECGroupTest.getTestGroups();
		Collection<Object[]> params = new ArrayList<Object[]>(testGroups.size());
		
		for (ECGroup grp : testGroups) {
			BigInteger skval = ECElGamal.generateSecretKey(grp, rand);
			ECElGamal.SK sk = new ECElGamal.SK(grp, skval);
			ECElGamal.PK pk = new ECElGamal.PK(grp, sk.getPK());
			
			
			Object[] param = {new ECElGamalTest(rand, grp, sk, pk)};
			params.add(param);
		}
		return params;
	}
	
	@RunWith(Parameterized.class)
	public static class Cipher extends ElGamalTest.Cipher<ECPoint> {
		public Cipher(ECElGamalTest globals) {
			super(globals);
		}
		
		@Parameters
		public static Collection<Object[]> getParameters() {
			return ECElGamalTest.getTestParameters();
		}
	}

	@RunWith(Parameterized.class)
	public static class Homomorphic extends ElGamalTest.Homomorphic<ECPoint> {
		public Homomorphic(ECElGamalTest globals) {
			super(globals);
		}
		
		@Parameters
		public static Collection<Object[]> getParameters() {
			return ECElGamalTest.getTestParameters();
		}
	}
}
