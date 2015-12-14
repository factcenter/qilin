package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.primitives.Homomorphic;
import org.factcenter.qilin.primitives.NonInteractiveCommitment;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertTrue;


abstract public class TrapdoorHomomorphicCommitmentPoKTest<C, P, R,
Com  extends NonInteractiveCommitment.Committer<C, P, R> & Homomorphic<C, P, R>,
Ver extends NonInteractiveCommitment.Verifier<C, P, R> & Homomorphic<C, P, R>> extends 
HomomorphicCommitmentPoKTest<C, P, R, Com, Ver> {

	abstract protected TrapdoorHomomorphicCommitmentPoK<C, P, R, Com, Ver>.TrapdoorProver getTrapdoorProver();
	
	protected TrapdoorHomomorphicCommitmentPoKTest(Random rand) {
		super(rand);
	}

	@Test
	public void testTrapdoorProofs()  throws IOException, InterruptedException {
		final TrapdoorHomomorphicCommitmentPoK<C, P, R, Com, Ver>.TrapdoorProver trapdoorProver = getTrapdoorProver();
		final HomomorphicCommitmentPoK<C, P, R, Com, Ver>.Verifier verifier = getVerifier();
		final Com committer = getCommitter();
		
		final ArrayList<TestValues> tests = new ArrayList<TestValues>(CONFIDENCE);
		
		final P arbitrary = getCommitPlaintext();

		for (int i = 0; i < CONFIDENCE; ++i) {
			TestValues test = new TestValues();
			
			test.plain = getCommitPlaintext();
			test.random = getCommitRandom();
			test.commit = committer.commit(arbitrary, test.random);
			tests.add(test);
		}
		

		Runnable verifierRunner = new Runnable() {
			@Override
			public void run() {
				verifier.setParameters(getVerifiertoProverChannel(), rand);
				try {
					verifier.init();
					for (TestValues test : tests) {
						boolean result = verifier.verifyValue(test.commit, test.plain);
						assertTrue(result);
					}
				} catch (IOException ioe) {
					assert false;
				} 
			}
		};

		Thread secondThread = new Thread(verifierRunner);
		secondThread.start();

		trapdoorProver.setParameters(getProvertoVerifierChannel(), rand);
		trapdoorProver.init();
		for (TestValues test : tests) {
			trapdoorProver.proveFakeValue(test.commit, test.plain);
		}

		secondThread.join();
	}


}
