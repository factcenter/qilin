package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.util.ByteEncoder;
import org.factcenter.qilin.primitives.RandomOracle;
import org.factcenter.qilin.primitives.concrete.DigestOracle;
import org.factcenter.qilin.util.EncodingUtils;
import org.factcenter.qilin.util.Pair;

/**
 * An implmentation of a Merkle tree using {@link RandomOracleCollisionResistantHash} as
 * the collision-resistant hash function. 
 * @author talm
 *
 */
public class RandomOracleMerkleTree extends MerkleTree<byte[]> {
	RandomOracle H;
	
	class HashType extends RandomOracleCollisionResistantHash<byte[], Pair<byte[],byte[]>> {
		public HashType(RandomOracle H,
				ByteEncoder<Pair<byte[], byte[]>> adapterFromP,
				ByteEncoder<byte[]> adapterToC) {
			super(H, adapterFromP, adapterToC);
		}
	}
	
	/**
	 * Encoding, but no decoding.
	 * @author talm
	 *
	 */
	class PairEncoder implements ByteEncoder<Pair<byte[], byte[]>> {
		@Override
		final public byte[] encode(Pair<byte[], byte[]> input) {
			int lenA = input.a == null ? 0 : input.a.length;
			int lenB = input.b == null ? 0 : input.b.length;
			int pos = 0;
			byte[] concat = new byte[4 + lenA + lenB];
			pos += EncodingUtils.encode(lenA, concat, pos);
			if (lenA > 0)
				System.arraycopy(input.a, 0, concat, pos, lenA);
			pos += lenA;
			if (lenB > 0)
				System.arraycopy(input.b, 0, concat, pos, lenB);
			
			return concat;
		}
		
		/**
		 * No decoding!
		 */
		@Override
		final public Pair<byte[], byte[]> decode(byte[] input) {
			assert false;
			return null;
		}

		/**
		 * No decoding!
		 */
		@Override
		final public Pair<byte[], byte[]> denseDecode(byte[] input) {
			assert false;
			return null;
		}

		@Override
		public int getMinLength() {
			assert false;
			return 0;
		}
	}
	
	public RandomOracleMerkleTree(RandomOracle H) {
		this.H = H;
		PairEncoder pairEncoder = new PairEncoder();
		EncodingUtils.ByteArrayEncoder byteDecoder = new EncodingUtils.ByteArrayEncoder(H.getNativeLength());
		hashfunc = new HashType(H, pairEncoder, byteDecoder); 
	}

	public RandomOracleMerkleTree() {
		this(new DigestOracle());
	}
	
	public int getNativeDigestLength() {
		return H.getNativeLength();
	}
}
