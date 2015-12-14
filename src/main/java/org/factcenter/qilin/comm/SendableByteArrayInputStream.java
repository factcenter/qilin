package org.factcenter.qilin.comm;

import java.io.ByteArrayInputStream;


/**
 * Convenience class for wrapping a {@link ByteArrayInputStream}.
 * @author talm
 *
 */
public class SendableByteArrayInputStream extends SendableInputStream {

	ByteArrayInputStream in;
	
	public SendableByteArrayInputStream(ByteArrayInputStream in) {
		super(in);
		this.in = in;
	}
	
	public SendableByteArrayInputStream(byte[] buf) {
		this(new ByteArrayInputStream(buf));
	}
}
