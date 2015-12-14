package org.factcenter.qilin.primitives.concrete;

import org.bouncycastle.math.ec.ECPoint;
import org.factcenter.qilin.primitives.generic.TrapdoorPedersenCommitment;
import org.factcenter.qilin.primitives.generic.TrapdoorPedersenCommitmentTest;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;


@RunWith(Enclosed.class)
public class ECTrapdoorPedersenTest extends TrapdoorPedersenCommitmentTest<ECPoint> {

	public ECTrapdoorPedersenTest(Random rand, ECGroup grp) {
		super(rand, grp, new ECTrapdoorPedersen(grp, TrapdoorPedersenCommitment.generateKey(grp, rand)));
	}
	

	public static Collection<Object[]> getTestParameters() {
		Random rand = new Random(1);
		List<ECGroup> testGroups = ECGroupTest.getTestGroups();
		Collection<Object[]> params = new ArrayList<Object[]>(testGroups.size());
		
		for (ECGroup grp : testGroups) {
			Object[] param = {new ECTrapdoorPedersenTest(rand, grp)};
			params.add(param);
		}
		return params;
	}

	@RunWith(Parameterized.class)
	public static class TrapdoorCommitment extends TrapdoorPedersenCommitmentTest.TrapdoorCommitment<ECPoint> {
		public TrapdoorCommitment(ECTrapdoorPedersenTest globals) {
			super(globals);
		}
		
		@Parameters
		public static Collection<Object[]> getTestParameters() {
			return ECTrapdoorPedersenTest.getTestParameters();
		}
	}

	@RunWith(Parameterized.class)
	public static class Homomorphic extends TrapdoorPedersenCommitmentTest.Homomorphic<ECPoint> {
		public Homomorphic(ECTrapdoorPedersenTest globals) {
			super(globals);
		}
		
		@Parameters
		public static Collection<Object[]> getTestParameters() {
			return ECTrapdoorPedersenTest.getTestParameters();
		}
	}
}
