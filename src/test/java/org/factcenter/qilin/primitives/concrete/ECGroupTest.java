package org.factcenter.qilin.primitives.concrete;

import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
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
public class ECGroupTest extends GroupTest<ECPoint> {
	ECGroup grp;
	Random rand;
	
	public static List<ECGroup> getTestGroups() {
		List<ECGroup> groups = new ArrayList<ECGroup>(2);
	
		// Create a tiny Elliptic Curve group of order 19:
		/*
		 * y^2 = x^3 + x + 5 (mod 37), Generator P=(28,9)
		 * 
			1 * P = (28,   9)	  2 * P = (30,   5)	  3 * P = (20, 12)	
  			4 * P = (14, 32)	  5 * P = (33, 23)	  6 * P = (  9, 22)	
  			7 * P = (10,   4)	  8 * P = (25, 35)	  9 * P = (18,   3)	
			10 * P = (18, 34)	11 * P = (25,   2)	12 * P = (10, 33)	
			13 * P = (  9, 15)	14 * P = (33, 14)	15 * P = (14,   5)	
			16 * P = (20, 25)	17 * P = (30, 32)	18 * P = (28, 28)	
			19 * P = Point at Infinity	
		*/	

		ECCurve curve = new ECCurve.Fp(BigInteger.valueOf(37), 
				BigInteger.ONE, BigInteger.valueOf(5)); 
		ECPoint G = curve.createPoint(BigInteger.valueOf(28), BigInteger.valueOf(9));
		ECParameterSpec spec = new ECParameterSpec(curve, G, BigInteger.valueOf(19));
		groups.add(new ECGroup(spec));

		// Create a large ECGroup (NIST recommended with reasonable security parameters)
		groups.add(new ECGroup("P-256"));
		return groups; 
	}
	
	/**
	 * Return parameters suitable for constructing an ECGroupTest object.
	 * Each item in the collection is an array of parameters, consisting
	 * of: {Random rand, ECGroup grp}
	 * @return the parameters. 
	 */
	@Parameters
	public static Collection<Object[]> getTestParams() {
		Random rand = new Random(1);
		List<ECGroup> testGroups = getTestGroups();
		Collection<Object[]> params = new ArrayList<Object[]>(testGroups.size());
		
		for (ECGroup grp : testGroups) {
			Object[] param = {rand, grp}; 
			params.add(param);
		}
		return params;
	}
	
	public ECGroupTest(Random rand, ECGroup grp) {
		this.rand = rand;
		this.grp = grp;
	}

	@Override
	protected ByteEncoder<ECPoint> getEncoder() {
		return grp;
	}

	@Override
	protected Group<ECPoint> getGroup() {
		return grp;
	}

	@Override
	protected Random getRand() {
		return rand;
	}
}
