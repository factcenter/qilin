package org.factcenter.qilin.primitives.generic;

import java.util.Random;




public class RandomOracleMerkleTreeTest extends MerkleTreeTest<byte[]> {
	RandomOracleMerkleTree tree;
	Random rand;
	
	public RandomOracleMerkleTreeTest() {
		tree = new RandomOracleMerkleTree();
		rand = new Random(1);
	}
	
	@Override
	protected byte[] getRandom() {
		byte[] hash = new byte[tree.getNativeDigestLength()];
		rand.nextBytes(hash);
		return hash;
	}

	@Override
	protected MerkleTree<byte[]> getTree() {
		return tree;
	}

	@Override
	protected byte[] getClone(byte[] orig) {
		return orig.clone();
	}
}
