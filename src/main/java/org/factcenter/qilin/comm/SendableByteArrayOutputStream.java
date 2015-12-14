package org.factcenter.qilin.comm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Convenience class for wrapping a {@link ByteArrayOutputStream}.
 * @author talm
 *
 */
public class SendableByteArrayOutputStream extends SendableOutputStream {
	ByteArrayOutputStream out;
	
	public SendableByteArrayOutputStream(ByteArrayOutputStream out) {
		super(out);
		this.out = out;
	}

	public SendableByteArrayOutputStream() {
		this(new ByteArrayOutputStream());
	}
	
	public void flush() {
		try {
			super.flush();
		} catch (IOException e) {
			// (Should never happen!)
			throw new RuntimeException ("Unexpected error in SendableByteArrayOutputStream" + e.getMessage());
		}
	}
	
	public byte[] toByteArray() {
		flush();
		return out.toByteArray();
	}
	
	/**
	 * Resets buffer.
	 * @see ByteArrayOutputStream#reset()
	 */
	public void reset() {
		flush();
		out.reset();
	}
	
}
