package org.factcenter.qilin.primitives.concrete;

import org.factcenter.qilin.primitives.generic.PedersenCommitmentTest;
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
public class ZpPedersenTest extends PedersenCommitmentTest<BigInteger> {
	public final static int[] BITS = {8, 64, 256}; 

	public ZpPedersenTest(Random rand, Zpsafe grp, BigInteger h) {
		super(rand, grp, new ZpPedersen(grp, h));
	}

	public static Collection<Object[]>  getTestParameters() {
		Random rand = new Random(1);
		List<Object[]> params = new ArrayList<Object[]>(BITS.length);
		for (int bits : BITS) {
			Zpsafe grp = new Zpsafe(Zpsafe.randomSafePrime(bits, 50, rand));
			BigInteger h;
			do {
				h = grp.sample(rand);
			} while (h.equals(BigInteger.ONE));
			Object[] param = {new ZpPedersenTest(rand, grp, h)};
			params.add(param);
		}
		return params;
	}
	

	@RunWith(Parameterized.class)
	public static class Commitment extends 
	PedersenCommitmentTest.Commitment<BigInteger>	{
		public Commitment(ZpPedersenTest globals) {
			super(globals);
		}

		@Parameters
		public static Collection<Object[]> getParams() {
			return ZpPedersenTest.getTestParameters();
		}
	}

	@RunWith(Parameterized.class)
	public static class Homomorphic extends 
					PedersenCommitmentTest.Homomorphic<BigInteger> {
		public Homomorphic(ZpPedersenTest globals) {
			super(globals);
		}

		@Parameters
		public static Collection<Object[]> getParams() {
			return ZpPedersenTest.getTestParameters();
		}
	}
}
