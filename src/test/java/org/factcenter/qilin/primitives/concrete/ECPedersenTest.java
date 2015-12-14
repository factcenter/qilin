package org.factcenter.qilin.primitives.concrete;

import org.bouncycastle.math.ec.ECPoint;
import org.factcenter.qilin.primitives.generic.PedersenCommitmentTest;
import org.factcenter.qilin.util.IntegerUtils;
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
public class ECPedersenTest extends PedersenCommitmentTest<ECPoint> {
	public ECPedersenTest(Random rand, ECGroup grp, ECPoint h) {
		super(rand, grp, new ECPedersen(grp, h));
	}
	

	public static Collection<Object[]> getTestParameters() {
		Random rand = new Random(1);
		List<ECGroup> testGroups = ECGroupTest.getTestGroups();
		Collection<Object[]> params = new ArrayList<Object[]>(testGroups.size());
		
		for (ECGroup grp : testGroups) {
			ECPoint g = grp.getGenerator();
			BigInteger order = grp.orderUpperBound();
			
			// Find another generator
			BigInteger hval;
			do {
				hval = IntegerUtils.getRandomInteger(order, rand);
			} while (!order.gcd(hval).equals(BigInteger.ONE));
			
			ECPoint h = grp.multiply(g, hval);
			Object[] param = {new ECPedersenTest(rand, grp, h)};
			params.add(param);
		}
		return params;
	}
	
	@RunWith(Parameterized.class)
	public static class Commitment extends PedersenCommitmentTest.Commitment<ECPoint> {
		public Commitment(ECPedersenTest globals) {
			super(globals);
		}
		
		@Parameters
		public static Collection<Object[]> getParameters() {
			return ECPedersenTest.getTestParameters();
		}
	}

	@RunWith(Parameterized.class)
	public static class Homomorphic extends PedersenCommitmentTest.Homomorphic<ECPoint> {
		public Homomorphic(ECPedersenTest globals) {
			super(globals);
		}
		
		@Parameters
		public static Collection<Object[]> getParameters() {
			return ECPedersenTest.getTestParameters();
		}
	}
}
