package org.factcenter.qilin.primitives;

/**
 * A collision-resistant hash. 
 * Given a "properly" created instance of a collision-resistant hash, it
 * should be computationally hard to find to different values in the
 * domain that map to the same value in the range. 
 * @author talm
 *
 * @param <C> Range of the hash function
 * @param <P> Domain of the hash function
 */
public interface CollisionResistantHash<C, P> {
	/**
	 * Compute the hash of an element.
	 * @param element
	 * @return the hash of element.
	 */
	public C hash(P element);
}
