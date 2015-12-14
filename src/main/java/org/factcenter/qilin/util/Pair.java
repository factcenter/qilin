package org.factcenter.qilin.util;

/**
 * A type-safe pair of elements.
 * @author talm
 *
 * @param <A> type of first element.
 * @param <B> type of second element.
 */
public class Pair<A, B> {
	final public A a;
	final public B b;
	
	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}

	@SuppressWarnings(value="unchecked")
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pair))
			return false;
		
		Pair<A,B> other = (Pair<A,B>) obj;
		
		return GenericsUtils.deepEquals(a, other.a) && GenericsUtils.deepEquals(b, other.b);
	}
}
