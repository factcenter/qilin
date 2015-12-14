package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.primitives.NonInteractiveCommitment.Verifier;
import org.factcenter.qilin.primitives.TrapdoorNonInteractiveCommitment.EquivocatingCommitter;
import org.factcenter.qilin.primitives.TrapdoorNonInteractiveCommitmentTest;
import org.junit.Ignore;

import java.math.BigInteger;
import java.util.Random;


public class TrapdoorPedersenCommitmentTest<G> extends PedersenCommitmentTest<G> {
	TrapdoorPedersenCommitment<G> trapdoorPedersen;

	public TrapdoorPedersenCommitmentTest(Random rand, Group<G> grp, TrapdoorPedersenCommitment<G> trapdoorPedersen) {
		super(rand, grp, trapdoorPedersen);
		this.trapdoorPedersen = trapdoorPedersen;
	}
	
	@Ignore
	public static class TrapdoorCommitment<G> extends TrapdoorNonInteractiveCommitmentTest<G,BigInteger,BigInteger> {
		TrapdoorPedersenCommitmentTest<G> globals;

		public TrapdoorCommitment(TrapdoorPedersenCommitmentTest<G> globals) {
			this.globals = globals;
		}

		@Override
		protected EquivocatingCommitter<G, BigInteger, BigInteger> getCommitter() {
			return globals.trapdoorPedersen;
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
}
