package org.factcenter.qilin.util;

/**
 * A type-safe triplet of elements.
 * @author talm
 *
 * @param <A> type of first element.
 * @param <B> type of second element.
 * @param <C> type of third element.
 */
public class Triplet<A, B, C> extends Pair<A,B> {
	final public C c;
	
	public Triplet(A a, B b, C c) {
		super(a,b);
		this.c = c;
	}

	@SuppressWarnings(value="unchecked")
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Triplet))
			return false;
		
		Triplet<A,B,C> other = (Triplet<A,B,C>) obj;
		
		return super.equals(other) && GenericsUtils.deepEquals(c, other.c);
	}
}
