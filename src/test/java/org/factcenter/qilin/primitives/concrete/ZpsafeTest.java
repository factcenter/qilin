package org.factcenter.qilin.primitives.concrete;

import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.primitives.GroupTest;
import org.factcenter.qilin.util.ByteEncoder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;


@RunWith(Parameterized.class)
public class ZpsafeTest extends GroupTest<BigInteger> {
	public final static int[] BITS = {8, 64, 256};
	
	Zpsafe grp;
	Random rand;
	
	public ZpsafeTest(Random rand, Zpsafe grp) {
		this.rand = rand;
		this.grp = grp;
	}


	@Parameters
	public static Collection<Object[]>  getTestParameters() {
		Random rand = new Random(1);
		List<Object[]> params = new ArrayList<Object[]>(BITS.length);
		for (int bits : BITS) {
			Zpsafe grp = new Zpsafe(Zpsafe.randomSafePrime(bits, 50, rand));
			Object[] param = {rand, grp};
			params.add(param);
		}
		return params;
	}
	
	
	@Override
	protected ByteEncoder<BigInteger> getEncoder() {
		return grp;
	}

	@Override
	protected Group<BigInteger> getGroup() {
		return grp;
	}

	@Override
	protected Random getRand() {
		return rand;
	}

}
