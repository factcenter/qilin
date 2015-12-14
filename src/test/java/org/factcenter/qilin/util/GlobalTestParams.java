package org.factcenter.qilin.util;

/**
 * Global parameters used by JUnit tests.
 * @author talm
 *
 */
public interface GlobalTestParams {
	/**
	 * Confidence value for tests 
	 * (probabilistic events should occur within this many tries for test to pass)
	 */
	final public static int CONFIDENCE = 30; 
}
