package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.protocols.CheatingPeerException;
import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.primitives.NonInteractiveCommitment;
import org.factcenter.qilin.protocols.TwoPartyGroupElementFlip;
import org.factcenter.qilin.util.StreamEncoder;

import java.io.IOException;

/**
 * Use the Blum "coin-flip over the telephone" to randomly select a group
 * element. Neither side (by itself) will know any additional side information
 * about the selected element (e.g., the discrete log of the selected element
 * will be hidden from both sides). 
 * 
 * @author talm
 *
 * @param <G> Group element that will be returned from the flip
 * @param <C> Type of commitment 
 * @param <R> Type of commitment's randomness
 */
public class BlumTwoPartyGroupElementFlip<G, C, R> {
	protected Group<G> grp;
	protected StreamEncoder<G> groupEncoder;
	protected StreamEncoder<C> commitEncoder;
	protected StreamEncoder<R> commitRandomnessEncoder;
	
	public BlumTwoPartyGroupElementFlip(Group<G> grp, 
			StreamEncoder<G> groupEncoder, StreamEncoder<C> commitEncoder,
			StreamEncoder<R> commitRandomnessEncoder) {
		this.grp = grp;
		this.groupEncoder = groupEncoder;
		this.commitEncoder = commitEncoder;
		this.commitRandomnessEncoder = commitRandomnessEncoder;
	}
	
	/**
	 * Represents the first party in the coin flip. 
	 *
	 */
	public class First extends ProtocolPartyBase implements TwoPartyGroupElementFlip.First<G> {
		NonInteractiveCommitment.Committer<C, G, R> committer;
		
		First(NonInteractiveCommitment.Committer<C, G, R> committer) {
			this.committer = committer;
		}
		
		public G flip() throws IOException {
			G initial = grp.sample(rand);
			R initialRandomness = committer.getRandom(rand);
			C initialCommit = committer.commit(initial, initialRandomness);
			
			commitEncoder.encode(initialCommit, out);
			out.flush();
			
			G value = groupEncoder.decode(in);
			
			groupEncoder.encode(initial, out);
			commitRandomnessEncoder.encode(initialRandomness, out);
			out.flush();
			
			return grp.add(initial, value);
		}
	}
	
	public First newFirst(NonInteractiveCommitment.Committer<C, G, R> committer) {
		return new First(committer);
	}

	/**
	 * Represents the second party in the coin flip. 
	 *
	 */
	public class Second extends ProtocolPartyBase implements TwoPartyGroupElementFlip.Second<G> {
		NonInteractiveCommitment.Verifier<C, G, R> verifier;
		
		Second(NonInteractiveCommitment.Verifier<C, G, R> verifier) {
			this.verifier = verifier;
		}
		
		public G flip() throws IOException {
			C initialCommit = commitEncoder.decode(in);
			
			if (!verifier.verifyCommitment(initialCommit))
				throw new CheatingPeerException("peer sent bad initial commitment");

			G value = grp.sample(rand);
			groupEncoder.encode(value, out);
			
			out.flush();
			
			G initial = groupEncoder.decode(in);
			R initialRandomness = commitRandomnessEncoder.decode(in);
			
			if (!verifier.verifyOpening(initialCommit, initial, initialRandomness))
				throw new CheatingPeerException("peer did not open commitment correctly");
			
			return grp.add(initial, value);
		}
	}
	
	public Second newSecond(NonInteractiveCommitment.Verifier<C, G, R> verifier) {
		return new Second(verifier);
	}
}
