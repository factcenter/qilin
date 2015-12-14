package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.primitives.CollisionResistantHash;
import org.factcenter.qilin.primitives.RandomOracle;
import org.factcenter.qilin.util.ByteEncoder;

/**
 * A collision-resistant hash based on a Random Oracle. The generic implementation
 * can be used to hash any domain to any range, as long as elements in the domain
 * can be serialized and elements in the range can be "densely" deserialized (i.e.,
 * any string of the appropriate length can be decoded to a valid element).
 * 
 * @author talm
 *
 * @param <C> the type of elements in the range of the hash function.
 * @param <P> the type of elements in the domain of the hash function.
 * 
 * @see RandomOracle
 */
public class RandomOracleCollisionResistantHash<C, P> implements CollisionResistantHash<C, P> {
	RandomOracle H;
	ByteEncoder<P> adapterFromP;
	ByteEncoder<C> adapterToC;
	int minlen;
	
	public RandomOracleCollisionResistantHash(RandomOracle H, 
			ByteEncoder<P> adapterFromP,
			ByteEncoder<C> adapterToC
			) {
		this.H = H;
		this.adapterFromP = adapterFromP;
		this.adapterToC = adapterToC;
		minlen = adapterToC.getMinLength();
	}
	
	@Override
	public C hash(P element) {
		byte[] hash = H.hash(adapterFromP.encode(element), minlen);
		return adapterToC.denseDecode(hash);
	}
}
