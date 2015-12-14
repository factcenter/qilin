package org.factcenter.qilin.protocols;

import java.io.IOException;

/**
 * A coin-flip for group elements where one of the parties may be able to use a <i>trapdoor</i>.
 * If the first party has a trapdoor key, it can determine in advance the outcome of the coin-flip.
 * The second party cannot tell whether or not the first party used the trapdoor.
 * 
 * @author talm
 *
 */
public interface TrapdoorTwoPartyGroupElementFlip extends TwoPartyGroupElementFlip {
	/**
	 * Represents a party holding a trapdoor key for the group-element flipping protocol. 
	 * 
	 * @param <G> group element type 
	 */
	public interface TrapdoorFirst<G> extends First<G> {
		public void trapdoorFlip(G outcome) throws IOException;
	}
}