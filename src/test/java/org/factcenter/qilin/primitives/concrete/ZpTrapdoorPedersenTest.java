package org.factcenter.qilin.primitives.concrete;

import org.factcenter.qilin.primitives.generic.TrapdoorPedersenCommitment;
import org.factcenter.qilin.primitives.generic.TrapdoorPedersenCommitmentTest;
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
public class ZpTrapdoorPedersenTest extends TrapdoorPedersenCommitmentTest<BigInteger> {
	public final static int[] BITS = {8, 64, 256}; 
	
	public ZpTrapdoorPedersenTest(Random rand, Zpsafe grp, BigInteger sk) {
		super(rand, grp, new ZpTrapdoorPedersen(grp, sk));
	}

	public static Collection<Object[]>  getTestParameters() {
		Random rand = new Random(1);
		List<Object[]> params = new ArrayList<Object[]>(BITS.length);
		for (int bits : BITS) {
			Zpsafe grp = new Zpsafe(Zpsafe.randomSafePrime(bits, 50, rand));
			BigInteger sk = TrapdoorPedersenCommitment.generateKey(grp, rand);
			Object[] param = {new ZpTrapdoorPedersenTest(rand, grp, sk)};
			params.add(param);
		}
		return params;
	}

	@RunWith(Parameterized.class)
	public static class Homomorphic extends 
	TrapdoorPedersenCommitmentTest.Homomorphic<BigInteger> {
		public Homomorphic(ZpTrapdoorPedersenTest globals) {
			super(globals);
		}

		@Parameters
		public static Collection<Object[]> getParams() {
			return ZpTrapdoorPedersenTest.getTestParameters();
		}
	}
	

	@RunWith(Parameterized.class)
	public static class TrapdoorCommitment extends 
	TrapdoorPedersenCommitmentTest.TrapdoorCommitment<BigInteger> {
		public TrapdoorCommitment(ZpTrapdoorPedersenTest globals) {
			super(globals);
		}

		@Parameters
		public static Collection<Object[]> getParams() {
			return ZpTrapdoorPedersenTest.getTestParameters();
		}
	}

}
