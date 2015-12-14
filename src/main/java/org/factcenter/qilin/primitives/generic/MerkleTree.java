package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.primitives.CollisionResistantHash;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.util.GenericsUtils;
import org.factcenter.qilin.util.Pair;
import org.factcenter.qilin.util.StreamEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Implmentation of a binary Merkle tree commitment using a generic collision-resistant hash.
 * @author talm
 *
 * @param <C> The type determining the range and domain of the collision-resistant hash.
 *  	The hash function must map a pair of elements of this type to a single element of
 *  	this type.  
 *  
 * @see CollisionResistantHash
 */
public class MerkleTree<C> {
	/**
	 * The hash function used to generate the Merkle tree.
	 */
	protected CollisionResistantHash<C, Pair<C, C>> hashfunc;

	/**
	 * The root of the Merkle Tree.
	 */
	protected Node root;
	
	/**
	 * The maximum length of a path from a leaf to the root.
	 */
	protected int depth;
	
	/**
	 * A map from leaf hashvalues to the nodes that represent them.
	 */
	Map<C, Node> leafToNode;
	
	/**
	 * A node in the Merkle tree. Each node has a value and pointers to its child nodes.
	 * The value is computed by hashing the values of the child nodes.
	 * @author talm
	 *
	 */
	protected class Node {
		/**
		 * Value of the node. This value is a commitment to the values of its children. 
		 */
		C hash;
		
		
		/**
		 * Parent of this node in the tree. The root has a null parent.
		 */
		Node parent;
		
		/**
		 * Pointer to left child. Leaves have null children.
		 */
		Node left;
		
		/**
		 * Number of leaves under the left child.
		 */
		int leftSubtreeSize;
		
		/**
		 * Pointer to right child. Leaves have null children.
		 */
		Node right;
		
		/**
		 * Number of leaves under the right child.
		 */
		int rightSubtreeSize;
		
		void updateHash() {
			C leftHash = left == null ? null : left.hash;
			C rightHash = right == null ? null : right.hash;
			hash = hashfunc.hash(new Pair<C, C>(leftHash, rightHash));
		}
		
	}
	
	protected MerkleTree() {
		root = new Node();
		depth = 0;
		leafToNode = new GenericsUtils.HashMap<C, Node>();
	}
	
	public MerkleTree(CollisionResistantHash<C, Pair<C, C>> hashfunc) {
		this();
		this.hashfunc = hashfunc;
	}
	
	public C getRoot() {
		return root.hash;
	}
	
	/**
	 * Add a leaf to the tree. 
	 * @param leafvalue
	 * @return true if the leaf was added, false if it was already in the tree.
	 */
	public boolean addLeaf(C leafvalue) {
		if (leafToNode.containsKey(leafvalue))
			return false;
		
		Node curNode = root;
		
		Node leafNode = new Node();
		leafNode.hash = leafvalue;
		leafToNode.put(leafvalue, leafNode);
		
		if (root.left == null) {
			assert root.right == null;
			root.left = leafNode;
			leafNode.parent = root;
			root.leftSubtreeSize = 1;
			root.updateHash();
			depth = 1;
		} else if (root.right == null) {
			root.right = leafNode;
			leafNode.parent = root;
			root.rightSubtreeSize = 1;
			root.updateHash();
		} else {
			/*
			 * Repeatedly choose the child with the smallest subtree until
			 * we reach a leaf.
			 */
			int curdepth = 0;
			while(curNode.leftSubtreeSize > 0) {
				if (curNode.leftSubtreeSize > curNode.rightSubtreeSize)
					curNode = curNode.right;
				else
					curNode = curNode.left;
				++curdepth;
			}
			
			assert curdepth <= depth;
			
			// We've reached a leaf.
			assert curNode.rightSubtreeSize == 0;
			
			// Create a new internal node, and insert it in place of curNode.
			// curNode will become the new left node, and the new leaf will become
			// its new right node.
			Node newInternal = new Node();
			newInternal.leftSubtreeSize = newInternal.rightSubtreeSize = 1;
			newInternal.parent = curNode.parent;
			if (curNode.parent.left == curNode) {
				curNode.parent.left = newInternal;
			} else {
				assert curNode.parent.right == curNode;
				curNode.parent.right = newInternal;
			}
			newInternal.left = curNode;
			newInternal.right = leafNode;
			leafNode.parent = curNode.parent = newInternal;
			newInternal.updateHash();
			
			
			if (curdepth == depth)
				++depth; 
			
			// Update the subtree counts and hashes on the path to the root.
			for (curNode = newInternal.parent; curNode != null; curNode = curNode.parent) {
				curNode.leftSubtreeSize = curNode.left.leftSubtreeSize + curNode.left.rightSubtreeSize;
				curNode.rightSubtreeSize = curNode.right.leftSubtreeSize + curNode.right.rightSubtreeSize;
				curNode.updateHash();
			}
		}
		
		return true;
	}
	
	/**
	 * Return a path from the leaf to the root.
	 * The path consists of a list of hash pairs, such that one element
	 * of each pair is a commitment to the two elements in the previous
	 * pair, and the first pair contains the leaf as one of the elements.  
	 * @param leaf
	 * @return the path from the leaf to the root, or null if the leaf 
	 * 			is not in the tree.
	 */
	public List<Pair<C,C>> getPath(C leaf) {
		Node curNode = leafToNode.get(leaf);
		if (curNode == null)
			return null;
		
		List<Pair<C,C>> path = new ArrayList<Pair<C,C>>(depth);
		for (curNode = curNode.parent; curNode != null; curNode = curNode.parent) {
			// The tree is left-leaning -- since there exists at least one node, 
			// the left child of every non-leaf node must exist.
			assert curNode.left != null;
			path.add(new Pair<C, C>(curNode.left.hash, curNode.right != null ? curNode.right.hash : null));
		}
		
		return path;
	}
	
	/**
	 * Verify that a path is a valid Merkle-tree path from a given leaf to a given
	 * tree root. 
	 * 
	 * @param leaf the leaf at which the path should start
	 * @param root the root of the tree to which the path root is compared.
	 * @param path the path to verify.
	 * @return true iff the first pair in the path contains the leaf, the hash of the final pair is the root,
	 * 	and each pair in the path contains the hash of the previous pair.
	 */
	public boolean verifyPath(C leaf, C root, List<Pair<C,C>> path) {
		// Verify that the each pair contains a commitment
		// to the previous pair and that the first pair 
		// in the path contains the leaf
		C lasthash = leaf;
		for (Pair<C,C> pair : path) {
			if (!GenericsUtils.deepEquals(pair.a, lasthash) && !GenericsUtils.deepEquals(pair.b, lasthash))
				return false;
			lasthash = hashfunc.hash(pair);
		}
		// Verify that the path ends in root.
		if (!GenericsUtils.deepEquals(lasthash, root))
			return false;
		
		// All the tests passed.
		return true;
	}

	/**
	 * This is a utility class for serializing a path.
	 * @author talm
	 *
	 */
	public class PathEncoder  implements StreamEncoder<List<Pair<C,C>>> {
		StreamEncoder<C> cEncoder;
		
		public PathEncoder(StreamEncoder<C> cEncoder) {
			this.cEncoder = cEncoder;
		}
		
//		@Override
//		public List<Pair<C, C>> decode(InputStream in) throws IOException {
//			int n = EncodingUtils.decodeInt(in);
//			List<Pair<C,C>> list = new ArrayList<Pair<C,C>>(n);
//			
//			for (int i = 0; i < n; ++i) {
//				C a = cEncoder.decode(in);
//				C b = cEncoder.decode(in);
//				list.add(new Pair<C, C>(a,b));
//			}
//			return list;
//		}
//
//		@Override
//		public int encode(List<Pair<C, C>> input, OutputStream out)
//		throws IOException {
//			int pos = 0;
//
//			// Encode length of list
//			pos += EncodingUtils.encode(input.size(), out);
//
//			// Encode list elements
//			for (Pair<C,C> item : input) {
//				pos += cEncoder.encode(item.a, out);
//				pos += cEncoder.encode(item.b, out);
//			}
//
//			return pos;
//		}

		@Override
		public void encode(List<Pair<C, C>> input, SendableOutput out)
				throws IOException {
			
			// Encode length of list
			out.writeInt(input.size());
			// Encode list elements
			for (Pair<C,C> item : input) {
				cEncoder.encode(item.a, out);
				cEncoder.encode(item.b, out);
			}
		}

		@Override
		public List<Pair<C, C>> decode(SendableInput in) throws IOException {
			int n = in.readInt();
			List<Pair<C,C>> list = new ArrayList<Pair<C,C>>(n); 

			for (int i = 0; i < n; ++i) {
				C a = cEncoder.decode(in);
				C b = cEncoder.decode(in);
				list.add(new Pair<C, C>(a,b));
			}
			return list;
		}
	}
}
