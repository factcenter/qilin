package org.factcenter.qilin.util;

/**
 * A version of {@link Pair} that implements Comparable (requires that the types are comparable).
 * @author talm
 *
 * @param <A> type of first element in pair.
 * @param <B> type of second element in pair.
 */
public class ComparablePair<A extends Comparable<A>, B extends Comparable<B>> extends Pair<A,B> implements Comparable<ComparablePair<A,B>> {
	public ComparablePair(A a, B b) {
		super(a, b);
	}

	@Override
	public int compareTo(ComparablePair<A, B> o) {
		int aCmp = a.compareTo(o.a);
		if (aCmp != 0)
			return aCmp;
		
		return b.compareTo(o.b);
	} 

}
