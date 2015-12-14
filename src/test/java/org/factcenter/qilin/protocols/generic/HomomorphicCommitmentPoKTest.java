package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.primitives.Homomorphic;
import org.factcenter.qilin.primitives.NonInteractiveCommitment;
import org.factcenter.qilin.util.GenericsUtils;
import org.factcenter.qilin.util.GlobalTestParams;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


abstract public class HomomorphicCommitmentPoKTest<C, P, R,
Com  extends NonInteractiveCommitment.Committer<C, P, R> & Homomorphic<C, P, R>,
Ver extends NonInteractiveCommitment.Verifier<C, P, R> & Homomorphic<C, P, R>> implements GlobalTestParams {
	protected Random rand;
	
	abstract protected HomomorphicCommitmentPoK<C, P, R, Com, Ver>.Prover getProver();
	abstract protected Channel getProvertoVerifierChannel();
	abstract protected Com getCommitter();
	abstract protected HomomorphicCommitmentPoK<C, P, R, Com, Ver>.Verifier getVerifier();
	abstract protected Channel getVerifiertoProverChannel();
	abstract protected P getCommitPlaintext();
	abstract protected R getCommitRandom();
	abstract protected BigInteger getPlaintextGroupOrder();
	
	protected HomomorphicCommitmentPoKTest(Random rand) {
		this.rand = rand;
	}
	
	class TestValues {
		P plain;
		R random;
		C commit;
	};
	
	class TestResults {
		int correctlyVerifiedProofs;
		int incorrectlyVerifiedProofs;
	}
	
	@Test
	public void testProofs()  throws IOException, InterruptedException {
		final HomomorphicCommitmentPoK<C, P, R, Com, Ver>.Prover prover = getProver();
		final HomomorphicCommitmentPoK<C, P, R, Com, Ver>.Verifier verifier = getVerifier();
		final Com committer = getCommitter();
		
		final ArrayList<TestValues> tests = new ArrayList<TestValues>(CONFIDENCE);

		for (int i = 0; i < CONFIDENCE; ++i) {
			TestValues test = new TestValues();
			
			test.plain = getCommitPlaintext();
			test.random = getCommitRandom();
			test.commit = committer.commit(test.plain, test.random);
			tests.add(test);
		}
		
		final TestResults testResults = new TestResults();
		
		Runnable verifierRunner = new Runnable() {
			@Override
			public void run() {
				verifier.setParameters(getVerifiertoProverChannel(), rand);
				try {
					verifier.init();
					for (TestValues test : tests) {
						// Test completeness
						boolean result = verifier.verifyValue(test.commit, test.plain);
						if (result)
							++testResults.correctlyVerifiedProofs;
						
						// Test soundness
						P badPlain = getCommitPlaintext();
						result = verifier.verifyValue(test.commit, badPlain);
						// If badPlain != test.plain then verification should
						// fail except with small probability
						if (result && !GenericsUtils.deepEquals(badPlain, test.plain))
							++testResults.incorrectlyVerifiedProofs;
					}
				} catch (IOException ioe) {
					assert false;
				}
			}
		};

		Thread secondThread = new Thread(verifierRunner);
		secondThread.start();

		prover.setParameters(getProvertoVerifierChannel(), rand);
		prover.init();
		
		for (TestValues test : tests) {
			prover.proveValue(test.commit, test.plain, test.random);
			prover.proveValue(test.commit, test.plain, test.random);
		}

		secondThread.join();
		
		assertEquals(tests.size(), testResults.correctlyVerifiedProofs);
		// We expect soundness test to fail with probability 1/group order
		float expectedSoundness = tests.size() / getPlaintextGroupOrder().floatValue();
		assertTrue("Expected " + expectedSoundness + " but got: " + testResults.incorrectlyVerifiedProofs,
				testResults.incorrectlyVerifiedProofs <= 4 * expectedSoundness);
	}
}
