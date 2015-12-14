package org.factcenter.qilin.comm;



/**
 * The Channel represents a point-to-point communication channel for sending and 
 * receiving data. Communication is guaranteed to be reliable and
 * in order. 
 *  
 * @author talm
 * @see LocalChannelFactory
 * @see TCPChannelFactory
 *
 */
public interface Channel extends SendableOutput, SendableInput {
	
}
