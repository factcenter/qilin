package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.protocols.ProtocolParty;

import java.io.IOException;
import java.util.Random;

/**
 * A default implementation of {@link ProtocolParty}.
 * @author talm
 *
 */
public class ProtocolPartyBase implements ProtocolParty {
	/**
	 * A communication channel to the peer.
	 */
	protected Channel toPeer;
	
	/**
	 * This is just a copy of {@link #toPeer} for convenience.
	 */
	protected SendableOutput out;
	
	/**
	 * This is just a copy of {@link #toPeer} for convenience.
	 */
	protected SendableInput in;
	
	
	/**
	 * Randomness generator.
	 */
	protected Random rand;

	/**
	 * Set the relevant class members.
	 */
	@Override
	public void setParameters(Channel toPeer, Random rand) {
		this.toPeer = toPeer;
		this.in = toPeer;
		this.out = toPeer;
		this.rand = rand;
	}

	/**
	 * Default initialization does nothing.
	 */
	@Override
	public void init() throws IOException {
		// Default init does nothing.
	}
}
