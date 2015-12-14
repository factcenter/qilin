package org.factcenter.qilin.protocols;

import java.io.IOException;

/**
 * Coin flip for group elements
 * (the result is a random group element).
 * @author talm
 *
 */
public interface TwoPartyGroupElementFlip {
	/**
	 * Interface for the "first" party in the two party
	 * protocol. The output for the first party is always 
	 * guaranteed to be a random group element.
	 * @author talm
	 *
	 * @param <G>
	 */
	public interface First<G> extends ProtocolParty {
		public G flip() throws IOException;
	}
	
	/**
	 * Interface for the "second" party in the two party
	 * protocol. The second party may output null instead
	 * of a group element (if the first party aborted or
	 * behaved maliciously).
	 * @author talm
	 *
	 * @param <G>
	 */
	public interface Second<G> extends ProtocolParty {
		public G flip() throws IOException;
	}
}
