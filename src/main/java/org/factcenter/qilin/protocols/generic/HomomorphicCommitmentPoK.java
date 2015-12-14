package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.primitives.NonInteractiveCommitment;
import org.factcenter.qilin.protocols.SigmaProtocol;
import org.factcenter.qilin.protocols.TwoPartyGroupElementFlip;
import org.factcenter.qilin.util.Triplet;
import org.factcenter.qilin.primitives.Homomorphic;
import org.factcenter.qilin.protocols.CommitmentPoK;
import org.factcenter.qilin.util.Pair;
import org.factcenter.qilin.util.StreamEncoder;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;



/**
 * A generic proof of knowledge that a homomorphic commitment can
 * be opened to a known value. 
 * 
 * @author talm
 *
 * @param <C> Commitment type (for messages)
 * @param <P> Message type (must be a group element)
 * @param <R> Randomness for commitment
 */
public class HomomorphicCommitmentPoK<C, P, R,
Com extends NonInteractiveCommitment.Committer<C, P, R> & Homomorphic<C, P, R>,
Ver extends NonInteractiveCommitment.Verifier<C, P, R> & Homomorphic<C, P, R>> {
	protected Group<P> grp;
	protected StreamEncoder<P> plainEncoder;
	protected StreamEncoder<C> commitEncoder;
	protected StreamEncoder<R> commitRandomEncoder;

	public HomomorphicCommitmentPoK(Group<P> grp, 
			StreamEncoder<P> plainEncoder, StreamEncoder<C> commitEncoder,
			StreamEncoder<R> commitRandomEncoder) {
		this.grp = grp;
		this.plainEncoder = plainEncoder;
		this.commitEncoder = commitEncoder;
		this.commitRandomEncoder = commitRandomEncoder;
	}
	
	/**
	 * Represents the prover in the commitment proof-of-knowledge protocol.
	 * @author talm
	 *
	 */
	public class Prover extends ProtocolPartyBase implements SigmaProtocol.Prover<Triplet<C,P,R>>, CommitmentPoK.Prover<C, P, R> {
		Com committer;
		protected TwoPartyGroupElementFlip.First<BigInteger> flipper;

		protected Prover(Com committer, TwoPartyGroupElementFlip.First<BigInteger> flipper) {
			this.committer = committer;
			this.flipper = flipper;
		}

		/**
		 * Override the default to also set the parameters for the internal coin-flip protocol.
		 */
		@Override
		public void setParameters(Channel toPeer, Random rand) {
			super.setParameters(toPeer, rand);
			flipper.setParameters(toPeer, rand);
		}
		

		/**
		 * Override the default to initialize the coin-flip protocol as well.
		 */
		@Override
		public void init() throws IOException {
			super.init();
			flipper.init();
		}
		
		/**
		 * Prove that the prover knows an opening of com1 to value plain1
		 * (rand1 is the randomness used to create com1) 
		 * @param com1
		 * @param plain1
		 * @param rand1
		 */
		public void proveValue(C com1, P plain1, R rand1) throws IOException {
			P b = grp.sample(rand); 
			R rB = committer.getRandom(rand);
			C bCom = committer.commit(b, rB);
			
			plainEncoder.encode(b, out);
			commitEncoder.encode(bCom, out);

			// Send (b, Com(b)) to verifier
			out.flush();

			// get a random challenge from verifier (we use
			// a coin flipping protocol to allow a trapdoor later)
			BigInteger chal = flipper.flip();

			R rMul = committer.rndmul(plain1, rand1, chal);
			R rSum = committer.rndadd(grp.multiply(plain1, chal), rMul, b, rB);

			commitRandomEncoder.encode(rSum, out);
			out.flush();
		}

		/**
		 * Call {@link #proveValue(Object, Object, Object)} with the parameters.
		 */
		@Override
		public void prove(Triplet<C,P,R> params) throws IOException {
			proveValue(params.a, params.b, params.c);
		}
	}
	
	/**
	 * Return a new prover.
	 * Subclasses can override this method to return the appropriate prover.
	 */
	public Prover newProver(Com committer, TwoPartyGroupElementFlip.First<BigInteger> flipper) {
		return new Prover(committer, flipper);
	}

	/**
	 * Represents the verifier in the commitment proof-of-knowledge protocol.
	 * @author talm
	 *
	 */
	public class Verifier extends ProtocolPartyBase implements SigmaProtocol.Verifier<Pair<C,P>>, CommitmentPoK.Verifier<C, P, R> {
		Ver verifier;
		TwoPartyGroupElementFlip.Second<BigInteger> flipper;

		protected Verifier(Ver verifier, TwoPartyGroupElementFlip.Second<BigInteger> flipper) {
			this.verifier = verifier;
			this.flipper = flipper;
		}

		@Override
		public void setParameters(Channel toPeer, Random rand) {
			super.setParameters(toPeer, rand);
			flipper.setParameters(toPeer, rand);
		}

		@Override
		public void init() throws IOException {
			super.init();
			flipper.init();
		}
		
		/**
		 * Verify that the prover knows an opening of com1 to value plain1
		 * @param com1
		 * @param plain1
		 * @return true iff the prover knows an opening of com1 to value plain1
		 */
		public boolean verifyValue(C com1, P plain1) throws IOException {
			P b = plainEncoder.decode(in);
			C bCom = commitEncoder.decode(in);
			
			if (!verifier.verifyCommitment(bCom))
				return false;
			
			BigInteger chal = flipper.flip();
			
			R rSum = commitRandomEncoder.decode(in);
			
			C testCommitmentMul = verifier.multiply(com1, chal);
			C testCommitment = verifier.add(testCommitmentMul, bCom);
			
			P plainMul = grp.multiply(plain1, chal);
			P expectedPlain = grp.add(plainMul, b);
			
			return verifier.verifyOpening(testCommitment, expectedPlain, rSum);
		}

		/**
		 * Call {@link #verifyValue(Object, Object)} with the parameters.
		 * 
		 */
		@Override
		public boolean verify(Pair<C,P> params) throws IOException {
			return verifyValue(params.a, params.b);
		}
	}

	public  Verifier newVerifier(Ver verifier, TwoPartyGroupElementFlip.Second<BigInteger> flipper) {
		return new Verifier(verifier, flipper);
	}
}
