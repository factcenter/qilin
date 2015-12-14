package org.factcenter.qilin.protocols;

import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

/**
 * An interface for performing more efficient OTs in a batch.
 * @author talm
 *
 */
public interface BulkOT {
	public interface Sender extends ProtocolParty {
		/**
		 * Do a block-OT (more efficient than doing them one-by-one).
		 * @param x0 Matrix of 0 choice sending bits (each row corresponds to the 0 choice of a single OT
		 * @param x1 Matrix of 1 choice sending bits (each row corresponds to the 1 choice of a single OT
		 * @throws IOException
		 */
		public void send(BitMatrix x0, BitMatrix x1) throws IOException;
	}

	public interface Receiver extends ProtocolParty  {

		/**
		 * Do a block-OT (more efficient than doing them one-by-one).
		 * @param choices a vector corresponding to the receiver's choices in each OT.
		 * @throws IOException
		 */
		public BitMatrix receive(BitMatrix choices) throws IOException;
	}

	/**
	 * An interface that allows the user to split the writing and reading phases
	 * of the send operation. This can be useful to reduce waiting times when several
	 * send/receive operations occur sequentially. This assumes the writing phase 
	 * occurs before the reading phase (if not, there is no point in implementing this interface).
	 * 
	 * @author talm
	 *
	 */
	public interface SplitSender extends ProtocolParty  {
		public interface State { }

		/**
		 * Initiate the split send operation (this phase should perform writes to the comm channel but no reads). 
		 *  
		 * To complete the operation, the user must call {@link #sendReadingPhase} with 
		 * the state returned by this method call.
		 * 
		 * @param x0
		 * @param x1
		 * @throws IOException
		 */
		public State sendWritingPhase(BitMatrix x0, BitMatrix x1) throws IOException;

		/**
		 * Completes the send operation initiated by {@link #sendWritingPhase(BitMatrix, BitMatrix)}.
		 * 
		 * @param state the return value given by {@link #sendWritingPhase(BitMatrix, BitMatrix)}.
		 * @throws IOException
		 */
		public void sendReadingPhase(State state) throws IOException;
	}

	/**
	 * An interface that allows the user to split the writing and reading phases
	 * of the receive operation. This can be useful to reduce waiting times when several
	 * send/receive operations occur sequentially. This assumes the writing phase 
	 * occurs before the reading phase (if not, there is no point in implementing this interface).
	 * @author talm
	 *
	 */
	public interface SplitReceiver extends ProtocolParty  {
		public interface State { }

		/**
		 * Start a receive operation This phase should perform writes to the comm channel but no reads. 
		 * 
		 * To complete the operation, the user must call {@link #receiveReadingPhase(State)} with
		 * the state returned by this method call.
		 * 
		 * @param choices a vector corresponding to the receiver's choices in each OT.
		 * @throws IOException
		 */
		public State receiveWritingPhase(BitMatrix choices) throws IOException;

		/**
		 * Completes the receive operation initiated by {@link #receiveWritingPhase(BitMatrix)}.
		 * 
		 * @param state
		 * @throws IOException
		 */
		public BitMatrix receiveReadingPhase(State state)  throws IOException;
	}
}
