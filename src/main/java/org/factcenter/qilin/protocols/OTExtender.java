package org.factcenter.qilin.protocols;

/**
 * A convenience interface to capture all the APIs that must be implemented by an OT Extender.
 * @author talm
 *
 */
public interface OTExtender extends OT1of2.Chooser, OT1of2.Sender, BulkOT.Sender, BulkOT.Receiver, BulkOT.SplitReceiver {
	

}
