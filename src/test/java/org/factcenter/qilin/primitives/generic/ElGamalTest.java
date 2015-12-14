package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.primitives.Cipher.PK;
import org.factcenter.qilin.primitives.Cipher.SK;
import org.factcenter.qilin.primitives.CipherSKTest;
import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.primitives.HomomorphicTest;
import org.factcenter.qilin.util.Pair;
import org.junit.Ignore;

import java.math.BigInteger;
import java.util.Random;


/**
 * Generic test class for ElGamal.
 * To use this class, subclass it with a concrete version. 
 * Annotate the subclass with "@RunWith(Enclosed.class)" and
 * create static inner classes that subclass Cipher and 
 * Homomorphic. Annotate the inner classes with
 * "@RunWith(Parameterized.class)". The subclassed inner classes
 * must contain a static method annotated with "@Parameters",
 * that returns a collection of arrays that correspond to 
 * the constructors' arguments.  
 *  
 * @author talm
 *
 * @param <G>
 */
public class ElGamalTest<G> {
	Random rand;
	Group<G> grp;
	ElGamal.PK<G> pk;
	ElGamal.SK<G> sk;
	
	protected ElGamalTest(Random rand, Group<G> grp, ElGamal.SK<G> sk,
						  ElGamal.PK<G> pk) {
		this.rand = rand;
		this.grp = grp;
		this.sk = sk;
		this.pk = pk;
	}

	@Ignore
	public static class Cipher<G> extends CipherSKTest<Pair<G, G>, G, BigInteger> {
		ElGamalTest<G> globals;

		public Cipher(ElGamalTest<G> globals) {
			this.globals = globals;
		}
		
		@Override
		protected G getRandomPlaintext() {
			return globals.grp.sample(globals.rand);
		}

		@Override
		public PK<Pair<G, G>, G, BigInteger> getPK() {
			return globals.pk;
		}
	
		@Override
		public SK<Pair<G, G>, G, BigInteger> getSK() {
			return globals.sk;
		}

		@Override
		protected Random getRand() {
			return globals.rand;
		}

		@Override
		protected float getCiphertextOrderApprox() {
			return globals.grp.orderUpperBound().floatValue();
		}
	}
	
	@Ignore
	public static class Homomorphic<G> extends HomomorphicTest<Pair<G, G>, G, BigInteger> {
		ElGamalTest<G> globals;

		public Homomorphic(ElGamalTest<G> globals) {
			this.globals = globals;
		}
		
		@Override
		protected PK<Pair<G, G>, G, BigInteger> getCipherPK() {
			return globals.pk;
		}

		@Override
		protected org.factcenter.qilin.primitives.Homomorphic<Pair<G, G>, G, BigInteger> getHom() {
			return globals.pk;
		}

		@Override
		protected Group<G> getPlaintextGroup() {
			return globals.grp;
		}
		
		@Override
		protected Random getRand() {
			return globals.rand;
		}
	}
}
