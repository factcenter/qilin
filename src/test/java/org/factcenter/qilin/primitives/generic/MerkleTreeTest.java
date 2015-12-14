package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.util.GenericsUtils;
import org.factcenter.qilin.util.GlobalTestParams;
import org.factcenter.qilin.util.Pair;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;



abstract public class MerkleTreeTest<C> implements GlobalTestParams {
	protected abstract MerkleTree<C> getTree();
	protected abstract C getRandom();
	protected abstract C getClone(C orig);

	/**
	 * Verify that the subtree is correctly linked and return the number of leaves
	 * under the subtree.
	 * @param node Root of subtree.
	 * @param leafValues a collection into which the leaf values are added. 
	 * @return true if the subtree was valid and false if it is invalidly linked.
	 */
	protected int getSubtreeLeaves(MerkleTree<C>.Node node, Collection<C> leafValues) {
		C leftHash;
		C rightHash;
		
		if (node.left != null) {
			assertEquals(node, node.left.parent);
			assertEquals(node.leftSubtreeSize, getSubtreeLeaves(node.left, leafValues));
			leftHash = node.left.hash;
		} else {
			assertEquals(0, node.leftSubtreeSize);
			leftHash = null;
		}
		
		if (node.right != null) {
			assertEquals(node, node.right.parent);
			assertEquals(node.rightSubtreeSize, getSubtreeLeaves(node.right, leafValues));
			rightHash = node.right.hash;
		} else {
			assertEquals(0, node.rightSubtreeSize);
			rightHash = null;
		}
		
		if (node.left == null && node.right == null) {
			// node is a leaf
			leafValues.add(node.hash);
			return 1;
		} else {
			Pair<C,C> hashpair = new Pair<C, C>(leftHash, rightHash);
			C actualHash = getTree().hashfunc.hash(hashpair);

			assertTrue(GenericsUtils.deepEquals(node.hash, actualHash));
			
			return node.leftSubtreeSize + node.rightSubtreeSize;
		}
	}
	
	@Test
	public void testTreeIntegrity() {
		MerkleTree<C> tree = getTree();
		
		ArrayList<C> elements = new ArrayList<C>(CONFIDENCE);
		
		for (int i = 0; i < CONFIDENCE; ++i) {
			elements.add(getRandom());
			tree.addLeaf(elements.get(i));
		}
		
		C root = tree.getRoot();
		assertNotNull(root);
		
		HashSet<C> verify = new HashSet<C>(CONFIDENCE);
		
		// Do a depth-first search
		assertEquals(CONFIDENCE, getSubtreeLeaves(tree.root, verify));
		
		assertEquals(CONFIDENCE, verify.size());

		assertTrue(verify.containsAll(elements));
	}

	
	@Test
	public void testPathVerification() {
		MerkleTree<C> tree = getTree();
		
		ArrayList<C> elements = new ArrayList<C>(CONFIDENCE);
		
		for (int i = 0; i < CONFIDENCE; ++i) {
			elements.add(getRandom());
			tree.addLeaf(elements.get(i));
		}
		
		C root = tree.getRoot();
		assertNotNull(root);
		
		// Check that we can retrieve elements with proofs.
		for (C leaf : elements) {
			List<Pair<C,C>> path = tree.getPath(leaf);
			assertNotNull(path);
			assertTrue(tree.verifyPath(leaf, root, path));
		}
	}
	

	@Test
	public void testPathology() {
		MerkleTree<C> tree = getTree();
		
		/** 
		 * Add many identical elements
		 */
		C element = getRandom();
		for (int i = 0; i < CONFIDENCE; ++i) {
			tree.addLeaf(element);
		}
		
		C root = tree.getRoot();
		assertNotNull(root);

		List<Pair<C,C>> path = tree.getPath(element);
		assertNotNull(path);
		assertTrue(tree.verifyPath(element, root, path));
	}
	
	@Test
	public void testClonedObjects() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		MerkleTree<C> tree = getTree();
		
		ArrayList<C> elements = new ArrayList<C>(CONFIDENCE);
		
		for (int i = 0; i < CONFIDENCE; ++i) {
			C element = getRandom();
			C elementClone = getClone(element);

			elements.add(element);
			tree.addLeaf(elementClone);
		}
		
		C root = tree.getRoot();
		assertNotNull(root);
		
		// Check that we can retrieve elements with proofs.
		for (C leaf : elements) {
			List<Pair<C,C>> path = tree.getPath(leaf);
			assertNotNull(path);
			assertTrue(tree.verifyPath(leaf, root, path));
		}
	}
}
