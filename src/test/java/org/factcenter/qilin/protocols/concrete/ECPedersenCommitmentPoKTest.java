package org.factcenter.qilin.protocols.concrete;

import org.bouncycastle.math.ec.ECPoint;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.primitives.concrete.ECGroup;
import org.factcenter.qilin.primitives.concrete.ECGroupTest;
import org.factcenter.qilin.primitives.concrete.ECPedersen;
import org.factcenter.qilin.primitives.concrete.ECTrapdoorPedersen;
import org.factcenter.qilin.primitives.generic.PedersenCommitment;
import org.factcenter.qilin.primitives.generic.TrapdoorPedersenCommitment;
import org.factcenter.qilin.protocols.generic.HomomorphicCommitmentPoK;
import org.factcenter.qilin.protocols.generic.TrapdoorHomomorphicCommitmentPoK;
import org.factcenter.qilin.protocols.generic.TrapdoorHomomorphicCommitmentPoKTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Random;


@RunWith(Parameterized.class)
public class ECPedersenCommitmentPoKTest extends 
TrapdoorHomomorphicCommitmentPoKTest<ECPoint, BigInteger, BigInteger, PedersenCommitment<ECPoint>, PedersenCommitment<ECPoint>> {
	ECPedersenCommitmentPoK commitPoK;
	ECPedersenCommitmentPoK verifyPoK;
	ECTrapdoorPedersen trapdoorPedersen;
	ECPedersen pedersen;
	Channel[] channels;
	
	public ECPedersenCommitmentPoKTest(Random rand, ECGroup grp) {
		super(rand);
	
		trapdoorPedersen = new ECTrapdoorPedersen(grp, TrapdoorPedersenCommitment.generateKey(grp, rand));
		pedersen = new ECPedersen(grp, trapdoorPedersen.getH());
		
		LocalChannelFactory channelFactory = new LocalChannelFactory();
		channels = channelFactory.getChannelPair();
		
		commitPoK = new ECPedersenCommitmentPoK(trapdoorPedersen);
		verifyPoK = new ECPedersenCommitmentPoK(pedersen);
	}
	
	@Parameters
	public static Collection<Object[]> getTestParameters() {
		return ECGroupTest.getTestParams();
	}
	
	@Override
	protected TrapdoorHomomorphicCommitmentPoK<ECPoint,BigInteger,BigInteger,PedersenCommitment<ECPoint>,PedersenCommitment<ECPoint>>.TrapdoorProver getTrapdoorProver() {
		return commitPoK.getTrapdoorProver();
	}

	@Override
	protected BigInteger getCommitPlaintext() {
		return pedersen.getRandom(rand);
	}

	@Override
	protected BigInteger getCommitRandom() {
		return pedersen.getRandom(rand);
	}

	@Override
	protected PedersenCommitment<ECPoint> getCommitter() {
		return pedersen;
	}

	@Override
	protected HomomorphicCommitmentPoK<ECPoint,BigInteger,BigInteger,PedersenCommitment<ECPoint>,PedersenCommitment<ECPoint>>.Prover getProver() {
		return getTrapdoorProver();
	}

	@Override
	protected HomomorphicCommitmentPoK<ECPoint,BigInteger,BigInteger,PedersenCommitment<ECPoint>,PedersenCommitment<ECPoint>>.Verifier getVerifier() {
		return verifyPoK.newVerifier();
	}

	@Override
	protected BigInteger getPlaintextGroupOrder() {
		return pedersen.getGroup().orderUpperBound();
	}

	@Override
	protected Channel getProvertoVerifierChannel() {
		return channels[0];
	}

	@Override
	protected Channel getVerifiertoProverChannel() {
		return channels[1];
	}
}
