package org.factcenter.qilin.protocols;

import java.io.IOException;

/**
 * A Sigma protocol is a three-move proof protocol with a specific structure:
 * <ol> 
 * 	<li>The prover commits</li>
 *  <li>The verifier sends a challenge.</li>
 *  <li>The prover responds to the challenge</li>
 * </ol> 
 * 
 */
public interface SigmaProtocol {
	/**
	 * An interface for a prover in a sigma protocol.
	 */
	public interface Prover<Params> extends ProtocolParty {
		/**
		 * Generate the response to a challenge message. Although not enforced
		 * by the compiler, the prove method must conform to the following
		 * communication scheme:
		 * <ol>
		 *   <li>Send a sequence of messages to the verifier.</li>
		 *   <li>Receive messages from the verifier (these must all be
		 * "public coin" messages).</li>
		 *   <li>Send a sequence of messages to the verifier</li>
		 * </ol>
		 * 
		 * @param params parameters used by the prover
		 * @throws IOException
		 */
		public void prove(Params params) throws IOException;
	}

	/**
	 * An interface for the verifier in a sigma protocol.
	 */
	public interface Verifier<Params> extends ProtocolParty {
		/**
		 * Although not enforced by the compiler, the verify method must conform
		 * to the following communication scheme:
		 * <ol>
		 *   <li> Receive a sequence of messages from the prover (the "commitment").</li>
		 *   <li> Send a sequence of messages to the prover (these must all be
		 *    "public coin" messages).</li>
		 *   <li> Receive a sequence of messages from the prover</li>
		 * </ol>
		 * 
		 * @param params parameters used by the verifier.
		 * @throws IOException
		 */
		public boolean verify(Params params) throws IOException;
	}
}
