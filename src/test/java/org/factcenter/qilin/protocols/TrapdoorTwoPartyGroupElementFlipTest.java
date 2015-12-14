package org.factcenter.qilin.protocols;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.util.Pair;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;



abstract public class TrapdoorTwoPartyGroupElementFlipTest<G> extends TwoPartyGroupElementFlipTest<G> {
	
	abstract protected TrapdoorTwoPartyGroupElementFlip.TrapdoorFirst<G> getTrapdoorFirst();
	
	protected TrapdoorTwoPartyGroupElementFlipTest(Random rand, Group<G> grp) {
		super(rand, grp);
	}

	@Test
	public void testTrapdoorFlip() throws IOException, InterruptedException {
		final TrapdoorTwoPartyGroupElementFlip.TrapdoorFirst<G> trapdoorFirst = getTrapdoorFirst();
		final TwoPartyGroupElementFlip.Second<G> second = getSecond();
		
		final List<G> chosen = new ArrayList<G>(CONFIDENCE);

		final Random rand = new Random();
		
		// Generate list of pre-chosen coinflips.
		for (int i = 0; i < CONFIDENCE; ++i) {
			chosen.add(grp.sample(rand));
		}

		// Create a "standard" flipper that forces the flips using the trapdoor.
		TwoPartyGroupElementFlip.First<G> forcedFlipper = new TwoPartyGroupElementFlip.First<G>() {
			int i = 0;
			
			@Override
			public G flip() throws IOException {
				G outcome = chosen.get(i++);
				trapdoorFirst.trapdoorFlip(outcome);
				return outcome;
			}

			@Override
			public void setParameters(Channel toPeer, Random rand) {
				trapdoorFirst.setParameters(toPeer, rand);
			}
			
			@Override
			public void init() throws IOException {
				trapdoorFirst.init();
			}
		};

		Pair<List<G>,List<G>> results = getFlips(forcedFlipper, second);
		
		analyzeResults(results);
	}
}
