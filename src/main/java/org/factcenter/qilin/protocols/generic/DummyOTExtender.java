package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;

/**
 * A Dummy implementation of the {@link OTExtender} interface that can be used for testing.
 * @author talm
 *
 */
public class DummyOTExtender implements OTExtender {
	
	Channel toPeer;

	@Override
	public byte[] receive(int idx) throws IOException {
		BitMatrix choices = BitMatrix.valueOf(idx, 1);

		return receive(choices).getPackedBits(false);
	}

	@Override
	public void setParameters(Channel toPeer, Random rand) {
		this.toPeer = toPeer; 
	}

	@Override
	public void init() throws IOException {
		// Nothing to do.
	}

	@Override
	public void send(byte[] x0, byte[] x1) throws IOException {
		BitMatrix x0b = new BitMatrix(x0, 0);
		BitMatrix x1b = new BitMatrix(x1, 0);
		send(x0b, x1b);
	}

	@Override
	public void send(BitMatrix x0, BitMatrix x1) throws IOException {
		// read the plaintext choices
		BitMatrix choices = toPeer.readObject(BitMatrix.class);
		
		BitMatrix results = new BitMatrix(x0.getNumCols(), x0.getNumRows());
		for (int i = 0; i < choices.getNumCols(); ++i) {
			if (choices.getBit(i) == 0) {
				results.copyRow(i, x0, i);
			} else {
				results.copyRow(i, x1, i);
			}
		}
		toPeer.writeObject(results);
		toPeer.flush();
	}

	@Override
	public BitMatrix receive(BitMatrix choices) throws IOException {
		State state = receiveWritingPhase(choices);
		BitMatrix results = receiveReadingPhase(state); 
		return results;
	}

	@Override
	public State receiveWritingPhase(BitMatrix choices) throws IOException {
		toPeer.writeObject(choices);
		toPeer.flush();
		return null;
	}

	@Override
	public BitMatrix receiveReadingPhase(State state) throws IOException {
		BitMatrix results = toPeer.readObject(BitMatrix.class);
		return results;
	}
}
