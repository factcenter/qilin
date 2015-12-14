package org.factcenter.qilin.primitives;

import org.junit.Test;

import static org.junit.Assert.assertTrue;


abstract public class TrapdoorNonInteractiveCommitmentTest<C, E, R> extends NonInteractiveCommitmentTest<C, E, R> {
	abstract protected TrapdoorNonInteractiveCommitment.EquivocatingCommitter<C, E, R> getCommitter();
	
	@Test
	public void testEquivocation() {
		TrapdoorNonInteractiveCommitment.EquivocatingCommitter<C, E, R> committer = getCommitter();
		NonInteractiveCommitment.Verifier<C, E, R> verifier = getVerifier();
		
		for (int i = 0; i < CONFIDENCE; ++i) {
			R rnd1 = getRandom();
			E el1 = getElement();
			C commit = committer.commit(el1, rnd1);
			
			// Sanity checks (make sure standard commitment worked)
			assertTrue(verifier.verifyCommitment(commit));
			assertTrue(verifier.verifyOpening(commit, el1, rnd1));
			
			E el2 = getElement();
			R rnd2 = committer.equivocate(el1, rnd1, el2);
			
			// check that equivocation works
			assertTrue(verifier.verifyOpening(commit, el2, rnd2));
		}
	}

}
