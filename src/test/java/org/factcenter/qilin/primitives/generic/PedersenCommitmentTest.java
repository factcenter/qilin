package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.primitives.Cipher.PK;
import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.primitives.HomomorphicTest;
import org.factcenter.qilin.primitives.NonInteractiveCommitment.Committer;
import org.factcenter.qilin.primitives.NonInteractiveCommitment.Verifier;
import org.factcenter.qilin.primitives.NonInteractiveCommitmentTest;
import org.factcenter.qilin.primitives.concrete.Zn;
import org.factcenter.qilin.util.IntegerUtils;
import org.junit.Ignore;

import java.math.BigInteger;
import java.util.Random;


public class PedersenCommitmentTest<G> {
	Random rand;
	Group<G> grp;
	PedersenCommitment<G> pedersen;

	public PedersenCommitmentTest(Random rand, Group<G> grp, PedersenCommitment<G> pedersen) {
		this.rand = rand;
		this.grp = grp;
		this.pedersen = pedersen;
	}
	
	protected BigInteger getRandomness() {
		return IntegerUtils.getRandomInteger(grp.orderUpperBound(), rand);
	}
	
	@Ignore
	public static class Commitment<G> extends NonInteractiveCommitmentTest<G,BigInteger,BigInteger> {
		PedersenCommitmentTest<G> globals;

		public Commitment(PedersenCommitmentTest<G> globals) {
			this.globals = globals;
		}

		@Override
		protected Committer<G, BigInteger, BigInteger> getCommitter() {
			return globals.pedersen;
		}

		@Override
		protected BigInteger getElement() {
			return getRandom();
		}

		@Override
		protected Verifier<G, BigInteger, BigInteger> getVerifier() {
			return globals.pedersen;
		}

		@Override
		protected BigInteger getRandom() {
			return globals.getRandomness();
		}
	}

	@Ignore
	public static class Homomorphic<G> extends
			HomomorphicTest<G,BigInteger,BigInteger> {
		PedersenCommitmentTest<G> globals;
		Zn plaintextGroup;

		public Homomorphic(PedersenCommitmentTest<G> globals) {
			this.globals = globals;
			plaintextGroup = new Zn(globals.grp.orderUpperBound());
		}

		@Override
		protected PK<G, BigInteger, BigInteger> getCipherPK() {
			return globals.pedersen;
		}

		@Override
		protected org.factcenter.qilin.primitives.Homomorphic<G, BigInteger, BigInteger> getHom() {
			return globals.pedersen;
		}

		@Override
		protected Group<BigInteger> getPlaintextGroup() {
			return plaintextGroup;
		}

		@Override
		protected Random getRand() {
			return globals.rand;
		}

	}
}
