package org.factcenter.qilin.protocols;

import java.io.IOException;

/**
 * Interface for 1-out-of-2 string Oblivious Transfer (OT). 
 * A 1-out-of-2 string OT is a two-party protocol. One party
 * is the {@link Sender} while the other is the {@link Chooser}.  
 *
 */
public interface OT1of2 {
	/**
	 * Represents the Sender in a 1-out-of-2 string OT. 
	 *
	 */
	public interface Sender extends ProtocolParty {
		/**
		 * Execute an OT transaction as the Sender. The Chooser will be
		 * able to learn one (and only one) of the two inputs, while the   
		 * sender learns nothing about the Chooser's choice.
		 *  
		 * @param x0
		 * @param x1
		 * @throws IOException
		 * @see Chooser#receive(int)
		 */
		public void send(byte[] x0, byte[] x1) throws IOException;
	}

	/**
	 * Represents the Chooser in a 1-out-of-2 string OT
	 *
	 */
	public interface Chooser extends ProtocolParty {
		/**
		 * Execute an OT transaction as the Chooser. The Chooser
		 * selects which of the two Sender inputs to learn. The sender
		 * will not learn anything about the Chooser's input. 
		 * 
		 * @param idx Can be either 0 or 1 
		 * @return input x[idx] (where x is the input vector to {@link Sender#send(byte[], byte[])})
		 * @throws IOException
		 * @see Sender#send(byte[], byte[])
		 */
		public byte[] receive(int idx) throws IOException;
	}
}
