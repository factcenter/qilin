package org.factcenter.qilin.protocols.concrete;

import org.bouncycastle.math.ec.ECPoint;
import org.factcenter.qilin.primitives.concrete.ECTrapdoorPedersen;
import org.factcenter.qilin.primitives.concrete.ECGroup;
import org.factcenter.qilin.primitives.concrete.ECPedersen;
import org.factcenter.qilin.primitives.concrete.Zn;
import org.factcenter.qilin.primitives.generic.PedersenCommitment;
import org.factcenter.qilin.protocols.generic.TrapdoorHomomorphicCommitmentPoK;

import java.math.BigInteger;

/**
 * Elliptic-curve implementation of {@link TrapdoorHomomorphicCommitmentPoK} using a {@link PedersenCommitment} as the
 * trapdoor commitment scheme. 
 * @author talm
 *
 */
public class ECPedersenCommitmentPoK extends 
TrapdoorHomomorphicCommitmentPoK<ECPoint, BigInteger, BigInteger, PedersenCommitment<ECPoint>, PedersenCommitment<ECPoint>> {
	PedersenCommitment<ECPoint> committer;
	ECPedersenCoinflip pedersenFlip;
	
	public ECPedersenCommitmentPoK(ECPedersen committer) {
		this(committer, committer.getGroup(), new Zn(committer.getGroup().orderUpperBound()));
	}

	public ECPedersenCommitmentPoK(ECTrapdoorPedersen committer) {
		this(committer, committer.getGroup(), new Zn(committer.getGroup().orderUpperBound()));
	}

	private ECPedersenCommitmentPoK(PedersenCommitment<ECPoint> committer,
			ECGroup commitGroup, Zn	plainGroup) {
		super(plainGroup, plainGroup, commitGroup, plainGroup);
		this.committer = committer;
		pedersenFlip = new ECPedersenCoinflip(commitGroup);
	}
	
	public class TrapdoorProver extends
	TrapdoorHomomorphicCommitmentPoK<ECPoint, BigInteger, BigInteger, PedersenCommitment<ECPoint>, PedersenCommitment<ECPoint>>.TrapdoorProver {
		protected TrapdoorProver(ECTrapdoorPedersen trapdoorCommitter) {
			super(committer, pedersenFlip.newTrapdoorFirst(trapdoorCommitter));
		}
	}

	public TrapdoorProver getTrapdoorProver() {
		if (!(committer instanceof ECTrapdoorPedersen)) {
			throw new UnsupportedOperationException("No trapdoor");
		} else {
			return new TrapdoorProver((ECTrapdoorPedersen) committer);
		}
	}
	
	public class Prover extends 
	TrapdoorHomomorphicCommitmentPoK<ECPoint, BigInteger, BigInteger, PedersenCommitment<ECPoint>, PedersenCommitment<ECPoint>>.Prover {
		protected Prover() {
			super(committer, pedersenFlip.newFirst(committer));
		}
	}

	public Prover newProver() {
		return new Prover();
	}
	
	public class Verifier extends 
	TrapdoorHomomorphicCommitmentPoK<ECPoint, BigInteger, BigInteger, PedersenCommitment<ECPoint>, PedersenCommitment<ECPoint>>.Verifier {
		protected Verifier() {
			super(committer, pedersenFlip.newSecond(committer));
		}
	}
	
	public Verifier newVerifier() {
		return new Verifier();
	}
}
