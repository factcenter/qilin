package org.factcenter.qilin.protocols;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.util.GenericsUtils;
import org.factcenter.qilin.util.GlobalTestParams;
import org.factcenter.qilin.util.Pair;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;



abstract public class TwoPartyGroupElementFlipTest<G> implements GlobalTestParams {
	protected Random globalRand;
	protected Group<G> grp;

	abstract protected TwoPartyGroupElementFlip.First<G> getFirst();
	abstract protected Channel getFirsttoSecondChannel();
	abstract protected TwoPartyGroupElementFlip.Second<G> getSecond();
	abstract protected Channel getSecondtoFirstChannel();
	
	protected TwoPartyGroupElementFlipTest(Random rand, Group<G> grp) {
		this.globalRand = rand;
		this.grp = grp;
	}
	
	protected Pair<List<G>,List<G>> getFlips(TwoPartyGroupElementFlip.First<G> first,
			final TwoPartyGroupElementFlip.Second<G> second) throws IOException, InterruptedException {
		
		List<G> firstResults = new ArrayList<G>(CONFIDENCE);
		final List<G> secondResults = new ArrayList<G>(CONFIDENCE);
		
		Runnable secondRunner = new Runnable() {
			@Override
			public void run() {
				second.setParameters(getSecondtoFirstChannel(), globalRand);
				try {
					second.init();
					for (int i = 0; i < CONFIDENCE; ++i) {
						G result = second.flip();
						assertNotNull("Coinflip failed", result);
						secondResults.add(result);
					}
				} catch (IOException ioe) {
					assert false;
				} 
			}
		};
		
		Thread secondThread = new Thread(secondRunner);
		secondThread.start();

		first.setParameters(getFirsttoSecondChannel(), globalRand);
		first.init();
		for (int i = 0; i < CONFIDENCE; ++i) {
			G result = first.flip();
			firstResults.add(result);
		}
		
		secondThread.join();
		return new Pair<List<G>,List<G>>(firstResults, secondResults);
	}
	
	
	protected void analyzeResults(Pair<List<G>,List<G>> results) {
		HashSet<G> set = new HashSet<G>();
		
		float expSize = 0;
		float groupOrder = grp.orderUpperBound().floatValue(); 
		
		for (int i = 0; i < CONFIDENCE; ++i) {
			G firstRes = results.a.get(i);
			G secondRes = results.b.get(i);
			
			expSize += 1 - set.size()/groupOrder;
			
			assertTrue("Coinflip results aren't consistent", 
					GenericsUtils.deepEquals(firstRes, secondRes));
			
			assertTrue("Random element isn't member of group", grp.contains(firstRes));
			set.add(firstRes);
		}
		
		assertTrue("Random set is too small", set.size() > expSize / 4);
	}
	
	@Test
	public void testFlip() throws IOException, InterruptedException {
		final TwoPartyGroupElementFlip.First<G> first = getFirst();
		final TwoPartyGroupElementFlip.Second<G> second = getSecond();

		Pair<List<G>,List<G>> results = getFlips(first, second);
		
		analyzeResults(results);
	}
}
