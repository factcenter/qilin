package org.factcenter.qilin.primitives;

import java.util.Random;

/**
 * Represents a sampleable distribution.
 * @author talm
 *
 * @param <G> elements in the support of the distribution.
 */
public interface Sampler<G> {
	/**
	 * Sample an element from the distribution.
	 * @param rand
	 * @return a random element.
	 */
	G sample(Random rand);
}
