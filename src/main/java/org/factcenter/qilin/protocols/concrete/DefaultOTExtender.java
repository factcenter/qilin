package org.factcenter.qilin.protocols.concrete;

import org.factcenter.qilin.primitives.PseudorandomGenerator;
import org.factcenter.qilin.protocols.BulkOT;
import org.slf4j.LoggerFactory;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.primitives.concrete.DigestOracle;
import org.factcenter.qilin.primitives.concrete.ECGroup;
import org.factcenter.qilin.primitives.generic.BlockCipherPRG;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.protocols.ProtocolParty;
import org.factcenter.qilin.protocols.generic.OTExtensionServer;
import org.factcenter.qilin.protocols.generic.PrecomputedOTClient;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;

/**
 * A convenience class that constructs a (passively-secure) OT extender server/client pair with 
 * sensible defaults (use ECNaorPinkas as seeds, RC4 for PRG, default Qilin digest for random oracle, etc.).
 * 
 * To use, after constructing the class you call <b>both</b> {@link #setParameters(Channel, Random)} and
 * {@link #setServerParameters(Channel, Random)}. Note that the communication channels <b>must</b> be different 
 * (since they will be used in different threads).
 *   
 * The class's {@link #init()} method initializes the client and and runs the extension server in a separate (daemon) thread.
 * After calling {@link #init()}, {@link #send(byte[], byte[])} and {@link #receive(int)} will work, but may block at times while
 * the extension server is generating additional OTs.
 * @author talm
 *
 */
public class DefaultOTExtender implements ProtocolParty, OTExtender {
	int partyId;

	boolean initialized;

	PrecomputedOTClient otClient;
	OTExtensionServer otServer;

	Channel toPeer;
	Channel toServerPeer;

	Thread serverThread;

	/**
	 * Public Constructor
	 * @param k security parameter. Used as size of block for extension (minimum size is key-length in bits for the PRG)  
	 * @param m number of OTs in extended block. Must be greater than k
	 * @param lowWaterMark When client gets below this many precomputed OTs, the server will start generating more.
	 * @param highWaterMark When client has more than this many precomputed OTs, the server will rest.
	 * @param partyId The Id of the party (one party should use 0 for this parameter, the other should use 1).
	 */
	public DefaultOTExtender(int k, int m, int lowWaterMark, int highWaterMark, int partyId) {
		initialized = false;
		this.partyId = partyId;

        PseudorandomGenerator clientPrg = new BlockCipherPRG(); // Using default implementation.
        PseudorandomGenerator serverPrg = new BlockCipherPRG();

        DigestOracle clientH = new DigestOracle();
        DigestOracle serverH = new DigestOracle();

        ECGroup grp = new ECGroup("P-256");
        ECNaorPinkasOT seedOTs = new ECNaorPinkasOT(serverH, grp);

        otClient = new PrecomputedOTClient(partyId, lowWaterMark, clientPrg, clientH);
        otServer = new OTExtensionServer(k, m, partyId, highWaterMark, seedOTs.newSender(), seedOTs.newChooser(), serverPrg, serverH);

		otServer.setOTConsumer(otClient);
	}

    public void stopServer() {
        otServer.stopRunning();
    }

	/**
	 * Set the channel and randomness parameters for the client.
	 */
	@Override
	public void setParameters(Channel toPeer, Random rand) {
		otClient.setParameters(toPeer, rand);
		this.toPeer = toPeer; 
	}

	/**
	 * Set the channel and randomness parameters for the server.
	 */
	public void setServerParameters(Channel toPeer, Random rand) {
		otServer.setParameters(toPeer, rand);
		this.toServerPeer = toPeer;
	}

	/**
	 * Initialize the server.
	 * This method creates (and starts) a new thread. The thread will be marked daemon, 
	 * so it will not prevent the program from exiting.
	 * 
	 * <b>Note: you must call <i>both</i> {@link #setParameters(Channel, Random)} and {@link #setServerParameters(Channel, Random)}
	 * before calling this method</b>
	 */
	@Override
	public void init() throws IOException {
		assert(toPeer != null && toServerPeer != null);

		if (initialized)
			return;

		initialized = true;

		serverThread = new Thread("OT Extension Server-" + partyId) {
			public void run() {
				try {
					otServer.init();
					otServer.run();
				} catch (Exception e) {
					// TODO: Handle
					LoggerFactory.getLogger(DefaultOTExtender.class).
					error("OT Extension Server-{} dying due to Exception:", partyId, e);
				}
			}
		};

		serverThread.setDaemon(true);
		serverThread.start();

		otClient.init();
	}

	@Override
	public void send(byte[] x0, byte[] x1) throws IOException {
		otClient.send(x0, x1);
	}

	@Override
	public byte[] receive(int idx) throws IOException {
		return otClient.receive(idx);
	}

	/**
	 * Do a block-OT (more efficient than doing them one-by-one).
	 * @param x0 Matrix of 0 choice sending bits (each row corresponds to the 0 choice of a single OT
	 * @param x1 Matrix of 1 choice sending bits (each row corresponds to the 1 choice of a single OT
	 * @throws IOException
	 */
	public void send(BitMatrix x0, BitMatrix x1) throws IOException {
		otClient.send(x0, x1);
	}

	/**
	 * Do a block-OT (more efficient than doing them one-by-one).
	 * @param choices a vector corresponding to the receiver's choices in each OT.
	 * @throws IOException
	 */
	public BitMatrix receive(BitMatrix choices) throws IOException {
		return otClient.receive(choices);
	}

	@Override
	public BulkOT.SplitReceiver.State receiveWritingPhase(BitMatrix choices)
			throws IOException {
		return otClient.receiveWritingPhase(choices);
	}

	@Override
	public BitMatrix receiveReadingPhase(BulkOT.SplitReceiver.State state) throws IOException {
		return otClient.receiveReadingPhase(state);
	} 
}
