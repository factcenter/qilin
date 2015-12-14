package org.factcenter.qilin.primitives;

import org.factcenter.qilin.util.GenericsUtils;
import org.factcenter.qilin.util.GlobalTestParams;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


abstract public class NonInteractiveCommitmentTest<C, E, R> implements GlobalTestParams {
	abstract protected NonInteractiveCommitment.Committer<C, E, R> getCommitter();
	abstract protected NonInteractiveCommitment.Verifier<C, E, R> getVerifier();
	abstract protected R getRandom();
	abstract protected E getElement();
	
	@Test
	public void testCompleteness() {
		NonInteractiveCommitment.Committer<C, E, R> committer = getCommitter();
		NonInteractiveCommitment.Verifier<C, E, R> verifier = getVerifier();
		
		for (int i = 0; i < CONFIDENCE; ++i) {
			R rnd = getRandom();
			E el = getElement();
			C commit = committer.commit(el, rnd);
			
			assertTrue(verifier.verifyCommitment(commit));
			assertTrue(verifier.verifyOpening(commit, el, rnd));
		}
	}
	
	@Test
	public void testSoundness() {
		NonInteractiveCommitment.Committer<C, E, R> committer = getCommitter();
		NonInteractiveCommitment.Verifier<C, E, R> verifier = getVerifier();
		
		for (int i = 0; i < CONFIDENCE; ++i) {
			R rnd = getRandom();
			R rnd2 = getRandom();
			E el = getElement();
			E el2 = getElement();
			
			if (GenericsUtils.deepEquals(rnd, rnd2))
				continue;
			
			if (GenericsUtils.deepEquals(el, el2))
				continue;

			C commit = committer.commit(el, rnd);
			
			assertFalse(verifier.verifyOpening(commit, el2, rnd));
			assertFalse(verifier.verifyOpening(commit, el, rnd2));
		}
	}

}
