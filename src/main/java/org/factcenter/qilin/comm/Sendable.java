package org.factcenter.qilin.comm;

import java.io.IOException;

public interface Sendable {
	public void writeTo(SendableOutput out) throws IOException;
	
	public void readFrom(SendableInput in) throws IOException;
}
