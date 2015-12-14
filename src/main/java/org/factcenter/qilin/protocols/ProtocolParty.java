package org.factcenter.qilin.protocols;

import org.factcenter.qilin.comm.Channel;

import java.io.IOException;
import java.util.Random;

/**
 * A ProtocolParty defines the basic functions every participant in a protocol should implement. 
 *
 */
public interface ProtocolParty {
	/**
	 * Set the channel used to communicate with the peer and the randomness used to generate messages.
	 * @param toPeer
	 */
	public void setParameters(Channel toPeer, Random rand);
	
	/**
	 * Perform any required initialization. This might involve communication with the peer.
	 * This method is meant for setup that is performed once; "setup" that is performed for every protocol execution 
	 * should be done in the protocol itself.
	 * @throws IOException
	 */
	public void init() throws IOException;

}
