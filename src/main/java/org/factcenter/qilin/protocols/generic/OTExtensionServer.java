package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.primitives.PseudorandomGenerator;
import org.factcenter.qilin.primitives.StreamingRandomOracle;
import org.factcenter.qilin.protocols.OT1of2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.factcenter.qilin.protocols.ProtocolParty;
import org.factcenter.qilin.util.BitMatrix;
import org.factcenter.qilin.util.EncodingUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;


/**
 * A Server that handles the generation of precomputed OTs. 
 * Note that the server must have  a <i>separate</i> communication channel 
 * from clients in a different thread so that OTs can be generated asynchronously.
 *  
 * @author talm
 *
 */
public class OTExtensionServer extends ProtocolPartyBase implements Runnable {
    final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * The PRG used to extend {@link #k}-bit OT intro {@link #m}-bit OT.
	 */
	PseudorandomGenerator prg;

	/**
	 * The random oracle used in the construction.
	 */
	StreamingRandomOracle H;
	
	/**
	 * The maximum number of precomputed OTs after which the OT generator rests.
	 */
	int highWaterMark;

	/**
	 * Which party we are in the protocol.
	 * One party should be 0, the other 1.
	 */
	int partyId;
	
	
	/**
	 * Seed OT instances for Sender side.
	 */
	OT1of2.Sender senderSeeds;

	/**
	 * Seed OT instances for Chooser side.
	 */
	OT1of2.Chooser chooserSeeds;

	/**
	 * The security parameter. This should be long enough to serve as the seed for a PRG.
	 */
	int k;

	/**
	 * The number of OTs generated in a block. The extender uses {@link #k} OTs of {@link #m}-bit strings to generate 
	 * precomputed {@link #m} OTs of length {@link #k}. 
	 */
	int m;


    /**
     * Number of choice OTs to keep in reserve
     */
    int choiceOTreserve;

    /**
     * Choice OTs reserved for extension
     */
    PrecomputedOTQueue<PrecomputedChoiceOTBlock> choiceOTs;


    /**
     * Number of sending OTs to keep in reserve
     */
    int sendingOTreserve;

    /**
     * Sending OTs reserved for extension
     */
    PrecomputedOTQueue<PrecomputedSendingOTBlock> sendingOTs;

	/**
	 * OT client used only for the extension protocol.
	 * We need our own private client to prevent thread safety issues
     * (in particular, prg and comm channel must be thread-local).
	 */
	PrecomputedOTClient otClientForExtension;  

	/**
	 * This OT client is the consumer of the "extra" (beyond those needed for the
	 * next extension) precomputed blocks generated
	 * by the extension server.
	 */
	PrecomputedOTClient otConsumer;
	
	/**
	 * A flag to let the generator know to exit.
	 */
	boolean stopRunning;


    /**
     * Stop the server (may take a while to actually stop).
     */
    public void stopRunning() { stopRunning = true; }


	public OTExtensionServer(int k, int m, int partyId, int highWaterMark,
			OT1of2.Sender senderSeeds, OT1of2.Chooser chooserSeeds, 
			PseudorandomGenerator prg, StreamingRandomOracle H) {
		this.k = k;
		this.m = m;
		this.partyId = partyId;
		this.highWaterMark = highWaterMark;
		this.senderSeeds = senderSeeds;
		this.chooserSeeds = chooserSeeds;
		this.prg = prg;
		this.H = H;
		
		this.otClientForExtension = new PrecomputedOTClient(partyId, 0, prg, H);
        choiceOTs = otClientForExtension.getChoiceOTBlockQueue();
        sendingOTs = otClientForExtension.getSendingOTBlockQueue();
        choiceOTreserve = k;
        sendingOTreserve = k;
    }

	/**
	 * Set the associated OT consumer 
	 * (precomputed OTs will be added to this consumer when the extension is run).
	 * The consumer's {@link PrecomputedOTQueue#needOTCallback} callback will be
	 * set to wake this server.
	 * @param otConsumer
	 */
	public void setOTConsumer(PrecomputedOTClient otConsumer) {
		this.otConsumer = otConsumer;
		otConsumer.setNeedOTCallback(new PrecomputedOTQueue.RequestAdditionalOTCallback() {
			@Override
			public void makeRequest() {
				synchronized(OTExtensionServer.this) {
					OTExtensionServer.this.notify();
				}
			}
		});
	}

	/**
	 * Set the relevant class members for this class and for the seedOTFactory.
	 */
	@Override
	public void setParameters(Channel toPeer, Random rand) {
		super.setParameters(toPeer, rand);
		otClientForExtension.setParameters(toPeer, rand);
		senderSeeds.setParameters(toPeer, rand);
		chooserSeeds.setParameters(toPeer, rand);
	}
	
	/**
	 * Generate the initial blocks of OTs using the seed OTs.
	 * Note: {@link #init()} uses the channels set by the
	 * {@link ProtocolParty#setParameters(Channel, Random)} call on {@link #senderSeeds} and
	 *  {@link #chooserSeeds}; if this channel
	 */
	@Override
	public void init() throws IOException {
        logger.debug("Initializing seed OTs: start");
		otClientForExtension.init();
		if (partyId == 0) {
			senderSeeds.init();
			chooserSeeds.init();
			
			initSendingOTs();
			initChoiceOTs();
		} else {
			chooserSeeds.init();
			senderSeeds.init();
			
			initChoiceOTs();
			initSendingOTs();
		}
        logger.debug("Initializing seed OTs: done");
	}
	
	/**
	 * Initialize choice OTs using the seed choice OTs.
	 * @throws IOException
	 */
	void initChoiceOTs() throws IOException {
		BitMatrix choices = new BitMatrix(k);
		
		choices.fillRandom(rand);
		BitMatrix results = new BitMatrix(k, k);
		byte[] resultBits = results.getBackingArray();
		for (int i = 0; i < k; ++i) {
			byte[] result = chooserSeeds.receive(choices.getBit(i));
			System.arraycopy(result, 0, resultBits, results.getRowIndex(i), results.getUsedBytesPerRow());
		}
		
		PrecomputedChoiceOTBlock choiceBlock =  new PrecomputedChoiceOTBlock(choices, results);
        choiceOTs.addOTs(choiceBlock);
	}

	/**
	 * Initialize sending OTs using the seed sending OTs.
	 * @throws IOException
	 */
	void initSendingOTs() throws IOException {
		BitMatrix x0 = new BitMatrix(k,k);
		x0.fillRandom(rand);
		BitMatrix x1 = new BitMatrix(k,k);
		x1.fillRandom(rand);
		
		byte[] x0bits = x0.getBackingArray();
		byte[] x1bits = x1.getBackingArray();
		for (int i = 0; i < k; ++i) {
			byte[] x0row = Arrays.copyOfRange(x0bits, x0.getRowIndex(i), x0.getRowIndex(i) + x0.getUsedBytesPerRow());
			byte[] x1row = Arrays.copyOfRange(x1bits, x1.getRowIndex(i), x1.getRowIndex(i) + x1.getUsedBytesPerRow());
			senderSeeds.send(x0row, x1row);
		}
		
		PrecomputedSendingOTBlock sendingBlock = new PrecomputedSendingOTBlock(x0, x1);
        sendingOTs.addOTs(sendingBlock);
	}
	
	/**
	 * Generate a block of {@link #m} precomputed OTs, using {@link #k} existing OTs.
	 *  
	 */
	void extendChoiceOTs() throws IOException {
		// Note: we use the notations of Fig.1 in the OT extension paper
        logger.debug("Extending choice OTs ({}/{} snd/choice for extending, {}/{} for client",
                sendingOTs.getAvailableOTs(), choiceOTs.getAvailableOTs(),
                otConsumer.getAvailableSendingOTs(), otConsumer.getAvailableChoiceOTs());

        if (sendingOTs.getAvailableOTs() < k) {
            // We are out of sending OTs, which we need to extend choice OTs
            logger.debug("Out of sending OTs required for extending choice OTs, will extend sending OTs first");

            // Increase the size of the sending OT reserve if possible
            // (to prevent recurrences)
            if (otConsumer.getAvailableSendingOTs() > k) {
                if (sendingOTreserve * 2 < m)
                    sendingOTreserve *= 2;
                else {
                    sendingOTreserve += (m - sendingOTreserve) / 2;
                }
            }

            assert(choiceOTs.getAvailableOTs() >= k);
            extendSendingOTs();
        }

		// r is the random selection bit vector (input to the chooser)
		BitMatrix r = new BitMatrix(m);
		r.fillRandom(rand);

		BitMatrix T = new BitMatrix(m, k);
		T.fillRandom(rand);

		// Every row of T xored with r
		BitMatrix Tr = T.clone();

		for (int i = 0; i < Tr.getNumRows(); ++i) {
			Tr.xorRow(i, r, 0);
		}

		otClientForExtension.send(T, Tr);

		BitMatrix y0;
		BitMatrix y1;

		y0 = in.readObject(BitMatrix.class);
		y1 = in.readObject(BitMatrix.class);

		BitMatrix Tt = T.transpose();
		BitMatrix results = new BitMatrix(k,m);
		byte[] tmp = new byte[4];
		for (int i = 0; i < m; ++i) {
			EncodingUtils.encode(i, tmp, 0);
			H.reset();
			H.update(tmp, 0, 4);
			assert(Tt.isZeroPadded());
			H.update(Tt.getBackingArray(), Tt.getRowIndex(i), Tt.getUsedBytesPerRow());
			byte[] hbits = H.digest(0, k);
			BitMatrix hj = new BitMatrix(hbits, 0, k, 1);

			if (r.getBit(i) == 0) {
				results.copyRow(i, y0, i);
			} else {
				results.copyRow(i, y1, i);
			}
			results.xorRow(i, hj, 0);
		}
		
		PrecomputedChoiceOTBlock choiceBlock = new PrecomputedChoiceOTBlock(r, results);
        if (choiceOTs.getAvailableOTs() < choiceOTreserve) {
            // make sure we have k in reserve
            PrecomputedChoiceOTBlock reservedBlock = choiceBlock.remove(choiceOTreserve);
            choiceOTs.addOTs(reservedBlock);
        }
        otConsumer.getChoiceOTBlockQueue().addOTs(choiceBlock);
	}

	/**
	 * Extend a block of {@link #k} sending OTs into {@link #m} pre-computed OTs.
	 * @throws IOException
	 */
	void extendSendingOTs() throws IOException {
        logger.debug("Extending sending OTs ({}/{} snd/choice for extending, {}/{} for client",
                sendingOTs.getAvailableOTs(), choiceOTs.getAvailableOTs(),
                otConsumer.getAvailableSendingOTs(), otConsumer.getAvailableChoiceOTs());

        if (choiceOTs.getAvailableOTs() < k) {
            // We may be out of
            logger.debug("Out of choice OTs required for extending sending OTs, will extend choice OTs first");

            // Increase the size of the sending OT reserve if possible
            // (to prevent recurrences)
            if (otConsumer.getAvailableChoiceOTs() > k) {
                if (choiceOTreserve * 2 < m)
                    choiceOTreserve *= 2;
                else {
                    choiceOTreserve += (m - choiceOTreserve) / 2;
                }
            }

            assert(sendingOTs.getAvailableOTs() >= k);
            extendChoiceOTs();
        }
		// We send m random input strings; these are used for
		// m pre-computed OTs of k-bit strings.
		BitMatrix x0 = new BitMatrix(k, m);
		BitMatrix x1 = new BitMatrix(k, m);
		x0.fillRandom(rand);
		x1.fillRandom(rand);

		// Initialize a random vector s
		BitMatrix s = new BitMatrix(k);
		s.fillRandom(rand);

		// Invoke existing OT(k.m) primitive, acting as a receiver with input s.
		// The matrix Q has k rows of length m
		BitMatrix Q = otClientForExtension.receive(s);

		// The matrix Qt has m rows of length k
		BitMatrix Qt = Q.transpose();

		BitMatrix y0 = x0.clone();
		BitMatrix y1 = x1.clone();
		byte[] tmp = new byte[4];
		for (int i = 0; i < m; ++i) {
			EncodingUtils.encode(i, tmp, 0);

			H.reset();
			H.update(tmp, 0, 4);
			assert(Qt.isZeroPadded());
			H.update(Qt.getBackingArray(), Qt.getRowIndex(i), Qt.getUsedBytesPerRow());
			byte[] h0bytes = H.digest(0, k);
			BitMatrix h0 = new BitMatrix(h0bytes, 0, k, 1);
			y0.xorRow(i, h0, 0);

			BitMatrix qsi = s.clone();
			qsi.xorRow(0, Qt, i);
			H.reset();
			H.update(tmp, 0, 4);
			assert(qsi.isZeroPadded());
			H.update(qsi.getBackingArray(), 0, qsi.getUsedBytesPerRow());

			byte[] h1bytes = H.digest(0, qsi.getUsedBytesPerRow());
			BitMatrix h1 = new BitMatrix(h1bytes, 0, k, 1);
			y1.xorRow(i, h1, 0);
		}

		out.writeObject(y0);
		out.writeObject(y1);
		out.flush();

		PrecomputedSendingOTBlock sendingBlock = new PrecomputedSendingOTBlock(x0, x1);
        if (sendingOTs.getAvailableOTs() < sendingOTreserve) {
            PrecomputedSendingOTBlock reservedBlock = sendingBlock.remove(sendingOTreserve);
            sendingOTs.addOTs(reservedBlock);
        }

        otConsumer.getSendingOTBlockQueue().addOTs(sendingBlock);
	}


    final boolean needChoiceOTs() {
        return otConsumer.getAvailableChoiceOTs() - k < highWaterMark;
    }

    final boolean needSendingOTs() {
        return otConsumer.getAvailableSendingOTs() - k < highWaterMark;
    }


    enum Command {
        CMD_EXTEND_CHOICE,
        CMD_EXTEND_SENDING,
        CMD_STOP,
    }

    public void runMasterExtender() throws IOException {
        while(!stopRunning) {
            while (needChoiceOTs() || needSendingOTs()) {
                // We always extend both, even if we need just one, since the extender itself requires
                // sendingOTs to extend choiceOTs and vice versa.
                // do better.
                if (needChoiceOTs()) {
                    toPeer.writeObject(Command.CMD_EXTEND_CHOICE);
                    toPeer.flush();
                    extendChoiceOTs();
                }
                if (needSendingOTs()) {
                    toPeer.writeObject(Command.CMD_EXTEND_SENDING);
                    toPeer.flush();
                    extendSendingOTs();
                }
            }

            // We don't want to busy-loop, so we wait until the OT consumer thread
            // wakes us up.
            synchronized(this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        toPeer.writeObject(Command.CMD_STOP);
        toPeer.flush();
    }

    public void runClientExtender() throws IOException {
        while(!stopRunning) {
            Command cmd = toPeer.readObject(Command.class);
            logger.debug("OT Extender client received command: {}", cmd);
            switch(cmd) {
                case CMD_EXTEND_CHOICE:
                    // Other side is extending choice, we extend sending OTs
                    extendSendingOTs();
                    break;
                case CMD_EXTEND_SENDING:
                    // Other side is extending sending, we extend choice OTs
                    extendChoiceOTs();
                    break;
                default:
                    stopRunning = true;
                    break;
            }
        }
    }


	/**
	 * Generate new OT blocks whenever we run below the low-water mark.
	 * Stop when we reach the high-water mark.
	 * Make sure to call {@link ProtocolParty#setParameters(Channel, Random)} 
	 * and {@link #init()} before running.
	 */
	@Override
	public void run() {
		try {
            if (partyId == 0) {
                // We are the master extender
                runMasterExtender();
            } else {
                // We are a client extender
                runClientExtender();
            }
		} catch (IOException e) {
			// TODO: Deal with this!
            if (!stopRunning)
                logger.error("IO Exception: {}", e);
            else
                logger.info("Stopped OT Extension Server");
		}
	}

}
