package org.factcenter.qilin.protocols;

import java.io.IOException;

/**
 * This exception is thrown by a protocol party that detects cheating by its peer.
 *
 */
public class CheatingPeerException extends IOException {
	private static final long serialVersionUID = 1L;
	
	public CheatingPeerException(String msg) {
		super(msg);
	}
	
	public CheatingPeerException() {
		
	}

}
