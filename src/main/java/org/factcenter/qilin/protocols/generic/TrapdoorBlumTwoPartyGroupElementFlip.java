package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.primitives.TrapdoorNonInteractiveCommitment;
import org.factcenter.qilin.protocols.TrapdoorTwoPartyGroupElementFlip;
import org.factcenter.qilin.util.StreamEncoder;

import java.io.IOException;

/**
 * A trapdoor version of the {@link BlumTwoPartyGroupElementFlip}.
 * @author talm
 *
 * @param <G>
 * @param <C>
 * @param <R>
 */
public class TrapdoorBlumTwoPartyGroupElementFlip<G, C, R> extends
BlumTwoPartyGroupElementFlip<G, C, R> {
	public TrapdoorBlumTwoPartyGroupElementFlip(Group<G> grp,
			StreamEncoder<G> groupEncoder, StreamEncoder<C> commitEncoder,
			StreamEncoder<R> commitRandomnessEncoder) {
		super(grp, groupEncoder, commitEncoder, commitRandomnessEncoder);
	}
	
	/**
	 * Represents a first party that can force the coin-flip to any value using the trapdoor. 
	 * @author talm
	 *
	 */
	public class TrapdoorFirst extends First implements TrapdoorTwoPartyGroupElementFlip.TrapdoorFirst<G> {
		TrapdoorNonInteractiveCommitment.EquivocatingCommitter<C, G, R> committer;

		TrapdoorFirst(TrapdoorNonInteractiveCommitment.EquivocatingCommitter<C, G, R>  committer) {
			super(committer);
			this.committer = committer;
		}

		public void trapdoorFlip(G outcome) throws IOException {
			R initialRandomness = committer.getRandom(rand);
			C initialCommit = committer.commit(grp.zero(), initialRandomness);

			commitEncoder.encode(initialCommit, out);
			out.flush();

			G value = groupEncoder.decode(in);

			G initial = grp.add(outcome, grp.negate(value));
			R fakeRandomness = committer.equivocate(grp.zero(), initialRandomness, initial);

			groupEncoder.encode(initial, out);
			commitRandomnessEncoder.encode(fakeRandomness, out);
			out.flush();
		}
	}

	public TrapdoorFirst newTrapdoorFirst(TrapdoorNonInteractiveCommitment.EquivocatingCommitter<C, G, R> committer) {
		return new TrapdoorFirst(committer);
	}

}
