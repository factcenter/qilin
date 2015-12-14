package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.primitives.PseudorandomGenerator;
import org.factcenter.qilin.primitives.StreamingRandomOracle;
import org.factcenter.qilin.protocols.OTExtender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.factcenter.qilin.util.BitMatrix;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;

public class PrecomputedOTClient extends ProtocolPartyBase implements OTExtender {

    final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * The PRG used to extend k-bit OT into m-bit OT.
	 */
	PseudorandomGenerator prg;

	/**
	 * The random oracle used in the construction.
	 */
	StreamingRandomOracle H;
	
	/**
	 * The minimum number of precomputed OTs before the OT generator should be notified becomes active. 
	 */
	int lowWaterMark;

    /**
     * Number of precomputed choice OTs to keep in reserve (these will be used by the OT extender)
     */
    int reservedChoice;

    /**
     * Number of precomputed sending OTs to keep in reserve (these will be used by the OT extender)
     */
    int reservedSending;
	
	/**
	 * Which party we are in the protocol.
	 * One party should be 0, the other 1.
	 */
	int partyId;



    PrecomputedOTQueue<PrecomputedChoiceOTBlock> choiceOTs;

    PrecomputedOTQueue<PrecomputedSendingOTBlock> sendingOTs;

	public int getAvailableSendingOTs() {
        int available = sendingOTs.getAvailableOTs();
        return  available < reservedSending ? 0 : available - reservedSending;
	}


    public int getAvailableChoiceOTs() {
        int available = choiceOTs.getAvailableOTs();
        return  available < reservedChoice ? 0 : available - reservedChoice;
    }
	



	public void setNeedOTCallback(PrecomputedOTQueue.RequestAdditionalOTCallback needOTCallback) {
        choiceOTs.setNeedOTCallback(needOTCallback);
        sendingOTs.setNeedOTCallback(needOTCallback);
	}
	
	/**
	 * Constructor
	 * @param partyId whether we are party 0 or 1
	 * @param lowWaterMark if number of available OTs goes below this number, callback  is called to request more.
	 * 	methods will block indefinitely when no OTs are available.
	 * @param prg a {@link PseudorandomGenerator}. Note: PRGs keep state so should not be shared between threads.
	 * @param H a random oracle instance. Note: Oracles may keep state so should not be shared between threads.
	 */
	public PrecomputedOTClient(int partyId, int lowWaterMark,
			PseudorandomGenerator prg, StreamingRandomOracle H) {
		this.partyId = partyId;
		this.lowWaterMark = lowWaterMark;
		this.prg = prg;
		this.H = H;
        reservedChoice = 0;
        reservedSending = 0;
		sendingOTs = new PrecomputedOTQueue<>(lowWaterMark);
        choiceOTs = new PrecomputedOTQueue<>(lowWaterMark);
	}

    /**
     * Set number of reserved choice and sending OTs.
     *
     * The {@link #send(BitMatrix, BitMatrix)} and {@link #receive(BitMatrix)}
     * methods will not allow use of these (this is useful if the same queue is
     * used elsewhere, as is the case with {@link OTExtensionServer}).
     *
     * @param reservedChoice
     * @param reservedSending
     */
    public void setReserved(int reservedChoice, int reservedSending) {
        this.reservedChoice = reservedChoice;
        this.reservedSending = reservedSending;
    }

    PrecomputedOTQueue<PrecomputedChoiceOTBlock> getChoiceOTBlockQueue() {
        return choiceOTs;
    }

    PrecomputedOTQueue<PrecomputedSendingOTBlock> getSendingOTBlockQueue() {
        return sendingOTs;
    }

    void setOTBlockQueues(PrecomputedOTQueue<PrecomputedChoiceOTBlock> choiceOTs, PrecomputedOTQueue<PrecomputedSendingOTBlock> sendingOts) {
        this.choiceOTs = choiceOTs;
        this.sendingOTs = sendingOts;
    }

	/**
	 * Wait for the next precomputed choice OT block to become available.
	 * The returned block may contain less than the requested number of OTs
	 * @param numOTs numOTs number of OTs requested.
	 */
	PrecomputedChoiceOTBlock getChoiceOTs(int numOTs) {
        return choiceOTs.getOTs(numOTs, reservedChoice);
	}

	/**
	 * Wait for the next precomputed sending OT block to become available.
	 * The returned block may contain less than the requested number of OTs
	 * @param numOTs numOTs number of OTs requested.
	 */
	PrecomputedSendingOTBlock getSendingOTs(int numOTs) {
        return sendingOTs.getOTs(numOTs, reservedSending);
	}

	@Override
	public void send(byte[] x0, byte[] x1) throws IOException {
		BitMatrix x0vec = new BitMatrix(x0, 0);
		BitMatrix x1vec = new BitMatrix(x1, 0);

		send(x0vec, x1vec);
	}


	/**
	 * Use the precomputed random OTs to send. 
	 * This method performs a block of OTs at once: 
	 * each row in the matrices corresponds to a single OT. 
	 * 
	 * Note that the matrices must have the same size.
	 * Given a m*n matrix (m columns, n rows), n OTs of m-bit
	 * strings are performed.
	 * 
	 * @param x0 
	 * @param x1
	 * @throws IOException  
	 */
    @Override
	public void send(BitMatrix x0, BitMatrix x1) throws IOException {
		assert(x0.getNumRows() == x1.getNumRows() && x0.getNumCols() == x1.getNumCols());

		int numOTs = x0.getNumRows();
		int OTlen = x0.getNumCols();

		BitMatrix mask0 = new BitMatrix(OTlen, numOTs);
		BitMatrix mask1 = new BitMatrix(OTlen, numOTs);

		byte[] mask0bits = mask0.getBackingArray();
		byte[] mask1bits = mask1.getBackingArray();

		for (int i = 0; i < numOTs; ) {
			PrecomputedSendingOTBlock sendingOTs = getSendingOTs(numOTs - i);

			BitMatrix precompX0 = sendingOTs.getX(0);
			BitMatrix precompX1 = sendingOTs.getX(1);
			int keyLen = precompX0.getNumCols();
			assert(precompX1.getNumCols() == keyLen);

			for (int j = 0; j < sendingOTs.getNumOTs(); ++j) {
				// Prepare the mask bits.

				if (OTlen > keyLen) {
					
					// We use the keyLen bits given by the precomputed OT as a seed for a PRG to expand them to OTlen pseudorandom bits.
					assert(precompX0.isZeroPadded());
					SecretKey key0 = new SecretKeySpec(precompX0.getBackingArray(), precompX0.getRowIndex(j), 
							precompX0.getUsedBytesPerRow(), "");
					try {
						prg.setKey(key0);
						prg.getPRGBytes(mask0bits, mask0.getRowIndex(i), mask0.getUsedBytesPerRow());


						assert(precompX1.isZeroPadded());	
						SecretKey key1 = new SecretKeySpec(precompX1.getBackingArray(), precompX1.getRowIndex(j), 
								precompX1.getUsedBytesPerRow(), "");

						prg.setKey(key1);
						prg.getPRGBytes(mask1bits, mask1.getRowIndex(i), mask1.getUsedBytesPerRow());
					} catch (InvalidKeyException e) {
						throw new RuntimeException("Invalid key -- should never happen (PRG should always accept this key type): "
								+ e.getMessage());
					}
				} else {
					mask0.copyRow(i, precompX0, j);
					mask1.copyRow(i, precompX1, j);
				}
				++i;
			}
		}

		// Receive the vector of masked choice bits

		BitMatrix maskedChoices = in.readObject(BitMatrix.class);

		for (int i = 0; i < numOTs; ++i) {
			if (maskedChoices.getBit(i) == 0) {
				mask0.xorRow(i, x0, i);
				mask1.xorRow(i, x1, i);
			} else {
				mask0.xorRow(i, x1, i);
				mask1.xorRow(i, x0, i);
			}
		}

		out.writeObject(mask0);
		out.writeObject(mask1);
		out.flush();
	}


	/**
	 * Use the precomputed OTs to receive. 
	 * Performs a block of OTs at once.  
	 * @param choices a vector of choice bits. The number of OTs in the block is the number of columns in this vector.
	 * @return An m*n matrix (m columns, n rows), where n is the number of OTs and m the string length. Each row of the matrix
	 * 	is the result of an OT with the corresponding choice bit.   
	 * @throws IOException
	 */
    @Override
	public BitMatrix receive(BitMatrix choices) throws IOException {
		ReceiveState state = receiveWritingPhase(choices);
		return receiveReadingPhase(state);
	}

	@Override
	public byte[] receive(int idx) throws IOException {
		BitMatrix c = new BitMatrix(1);
		c.setBit(0, idx);
		
		BitMatrix results = receive(c);
		byte[] bits = results.getPackedBits(false);
		return bits;
	}

	class ReceiveState implements State {
		BitMatrix choices;
		BitMatrix maskKeys;
		BitMatrix maskedChoices; 
	}
	
	@Override
   	public ReceiveState receiveWritingPhase(BitMatrix choices)	throws IOException {

		ReceiveState state = new ReceiveState();
		state.choices = choices;
		
		int numOTs = choices.getNumCols();
		state.maskedChoices = choices.clone();

		state.maskKeys = null; 

		// Use precomputed OT to mask choices
		for (int i = 0; i < numOTs; ) {
			PrecomputedChoiceOTBlock choiceBlock = getChoiceOTs(numOTs - i);
			BitMatrix choiceResults = choiceBlock.getResults();
			
			// We copy the keys to a new BitMatrix to consolidate all the
			// blocks into a single one.
			if (state.maskKeys == null) {
				state.maskKeys = new BitMatrix(choiceResults.getNumCols(), numOTs);
			} 
			assert(state.maskKeys.getNumCols() == choiceResults.getNumCols());
			
			for (int j = 0; j < choiceBlock.getNumOTs(); ++j) {
				state.maskedChoices.xorBit(i + j, choiceBlock.getChoiceBit(j));

				state.maskKeys.copyRow(i + j, choiceResults, j); 
			}
			
			i += choiceBlock.getNumOTs();
		}

		out.writeObject(state.maskedChoices);
		out.flush();
		return state;
	}

	@Override
	public BitMatrix receiveReadingPhase(State bulkState) throws IOException {
		ReceiveState state = (ReceiveState) bulkState;
		int numOTs = state.choices.getNumCols();
		
		BitMatrix masked0 = in.readObject(BitMatrix.class);
		BitMatrix masked1 = in.readObject(BitMatrix.class);

		// We only know the number of columns now (this causes an extra allocation).
		assert(masked0.getNumCols() == masked1.getNumCols());
		int OTLen = masked0.getNumCols();

		assert(state.maskKeys.isZeroPadded());
		byte[] keyBits = state.maskKeys.getBackingArray();
		int keyLen = state.maskKeys.getNumCols();
		int keyLenBytes = state.maskKeys.getUsedBytesPerRow();

		BitMatrix results;

		if (OTLen > keyLen) {
			results = new BitMatrix(OTLen, numOTs);

			for (int i = 0; i < numOTs; ++i) {
				SecretKey key = new SecretKeySpec(keyBits, state.maskKeys.getRowIndex(i), keyLenBytes, "");
				try {
					prg.setKey(key);
				} catch (InvalidKeyException e) {
					throw new RuntimeException("Invalid key -- should never happen (PRG should always accept this key type): "
							+ e.getMessage());
				}
				prg.getPRGBytes(results.getBackingArray(), results.getRowIndex(i), results.getUsedBytesPerRow());
			}
			results.zeroPad();
		} else {
			results = state.maskKeys;
			results.subcolumns(0, OTLen);
		}

		for (int i = 0; i < numOTs; ++i) {
			if ((state.maskedChoices.getBit(i) ^ state.choices.getBit(i)) == 0) {
				results.xorRow(i, masked0, i);
			} else {
				results.xorRow(i, masked1, i);
			}
		}

		return results;
	}

}