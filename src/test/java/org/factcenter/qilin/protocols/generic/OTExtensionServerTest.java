package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.primitives.PseudorandomGenerator;
import org.factcenter.qilin.primitives.StreamingRandomOracle;
import org.factcenter.qilin.primitives.concrete.DigestOracle;
import org.factcenter.qilin.primitives.generic.BlockCipherPRG;
import org.factcenter.qilin.protocols.OT1of2;
import org.factcenter.qilin.util.BitMatrix;
import org.factcenter.qilin.util.Pair;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OTExtensionServerTest {
	Random rand;
	PseudorandomGenerator clientPrg0, serverPrg0, clientPrg1, serverPrg1 ;
	StreamingRandomOracle clientH0, serverH0, clientH1, serverH1;

	PrecomputedOTClient otClient0;
	PrecomputedOTClient otClient1;
	OTExtensionServer otExtender0;
	OTExtensionServer otExtender1;

	// These two are connected.
	Channel clientChannel0; 
	Channel clientChannel1;

	Channel serverChannel0;
	Channel serverChannel1;

	final int k = 80;
	final int m = 160;

	class DummyChooser extends ProtocolPartyBase implements OT1of2.Chooser {
		@Override
		public byte[] receive(int idx) throws IOException {

			byte[] x0 = in.readObject(byte[].class);
			byte[] x1 = in.readObject(byte[].class);

			return (idx == 0) ? x0 : x1;
		}
	}

	class DummySender extends ProtocolPartyBase implements OT1of2.Sender {
		@Override
		public void send(byte[] x0, byte[] x1) throws IOException {
			out.writeObject(x0);
			out.writeObject(x1);
			out.flush();
		}
	}



	@Before
	public void setup() {
		rand = new Random(0);
		clientPrg0 = new BlockCipherPRG();
		serverPrg0 = new BlockCipherPRG();
		clientH0 = new DigestOracle();
		serverH0 = new DigestOracle();
		clientPrg1 = new BlockCipherPRG();
		serverPrg1 = new BlockCipherPRG();
		clientH1 = new DigestOracle();
		serverH1 = new DigestOracle();

		LocalChannelFactory lcf = new LocalChannelFactory();
		Channel[] channels = lcf.getChannelPair();
		clientChannel0 = channels[0];
		clientChannel1 = channels[1];

		channels = lcf.getChannelPair();
		serverChannel0 = channels[0];
		serverChannel1 = channels[1];

		DummySender senderSeeds0 = new DummySender();
		senderSeeds0.setParameters(serverChannel0, rand);

		DummySender senderSeeds1 = new DummySender();
		senderSeeds1.setParameters(serverChannel1, rand);

		DummyChooser chooserSeeds0 = new DummyChooser();
		chooserSeeds0.setParameters(serverChannel0, rand);

		DummyChooser chooserSeeds1 = new DummyChooser();
		chooserSeeds1.setParameters(serverChannel1, rand);

		otClient0 = new PrecomputedOTClient(0, 1, clientPrg0, clientH0);
		otClient1 = new PrecomputedOTClient(1, 1, clientPrg1, clientH1);

		otExtender0 = new OTExtensionServer(k, m, 0, k*3, senderSeeds0, chooserSeeds0, serverPrg0, serverH0);
		otExtender0.setOTConsumer(otClient0);
		
		otExtender1 = new OTExtensionServer(k, m, 1, k*3, senderSeeds1, chooserSeeds1, serverPrg1, serverH1);
		otExtender1.setOTConsumer(otClient1);

		otClient0.setParameters(clientChannel0, rand);
		otClient1.setParameters(clientChannel1, rand);

		otExtender0.setParameters(serverChannel0, rand);
		otExtender1.setParameters(serverChannel1, rand);
	}


	/**
	 * Check that all precomputed OTs are correct 
	 * @param a checks choice OTs for a
	 * @param b checks sending OTs for b
	 */
	void verifyPrecomputedBlocks(PrecomputedOTClient a, PrecomputedOTClient b) {
		assertEquals(a.getAvailableChoiceOTs(), b.getAvailableSendingOTs());
        while (a.getAvailableChoiceOTs() > 0) {
            PrecomputedChoiceOTBlock choice = a.getChoiceOTs(a.getAvailableChoiceOTs());
            PrecomputedSendingOTBlock sending = b.getSendingOTs(b.getAvailableSendingOTs());

            for (int i = 0; i < choice.getNumOTs(); ++i) {
                int choiceBit = choice.getChoiceBit(i);
                for (int j = 0; j < choice.getResults().getNumCols(); ++j) {
                    assertEquals(sending.getX(choiceBit).getBit(j, i), choice.getResults().getBit(j, i));
                }
            }
        }
	}

	/**
	 * Test a single extension.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testExtension() throws IOException, InterruptedException {
		Pair<PrecomputedChoiceOTBlock, PrecomputedSendingOTBlock> block = PrecomputedOTClientTest.generateDummyBlock(m, k, rand);
		otExtender0.otClientForExtension.getChoiceOTBlockQueue().addOTs(block.a);
		otExtender1.otClientForExtension.getSendingOTBlockQueue().addOTs(block.b);

		Thread ext1 = new Thread("Extender 1 (Choice)") {
			public void run() {
				try {
					otExtender1.extendChoiceOTs();
				} catch (IOException e) {
					fail("Bad! " + e);
				}
			}
		};

		ext1.start();

		otExtender0.extendSendingOTs();

		ext1.join();

		verifyPrecomputedBlocks(otExtender1.otClientForExtension, otExtender0.otClientForExtension);

		verifyPrecomputedBlocks(otExtender1.otConsumer, otExtender0.otConsumer);
	}


	/**
	 * Test an extension from a k*k block.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testInit() throws IOException, InterruptedException {

		Thread ext1 = new Thread("Extender 1 (Choice)") {
			public void run() {
				try {
					otExtender1.init();
				} catch (Exception e) {
					fail("Bad! " + e);
				}
			}
		};

		ext1.start();

		otExtender0.init();

		ext1.join();

		verifyPrecomputedBlocks(otExtender1.otClientForExtension, otExtender0.otClientForExtension);
		verifyPrecomputedBlocks(otExtender0.otClientForExtension, otExtender1.otClientForExtension);
	}




	/**
	 * Test a multiple extensions.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testMultipleExtensions() throws IOException, InterruptedException {
		Pair<PrecomputedChoiceOTBlock, PrecomputedSendingOTBlock> block = PrecomputedOTClientTest.generateDummyBlock(m, k, rand);
		otExtender0.otClientForExtension.getChoiceOTBlockQueue().addOTs(block.a);
		otExtender1.otClientForExtension.getSendingOTBlockQueue().addOTs(block.b);
		block = PrecomputedOTClientTest.generateDummyBlock(m, k, rand);
		otExtender1.otClientForExtension.getChoiceOTBlockQueue().addOTs(block.a);
		otExtender0.otClientForExtension.getSendingOTBlockQueue().addOTs(block.b);

		Thread ext1 = new Thread("Extender 1 (Choice)") {
			public void run() {
				try {
					otExtender1.extendChoiceOTs();
					otExtender1.extendSendingOTs();

					otExtender1.extendChoiceOTs();
					otExtender1.extendSendingOTs();

					otExtender1.extendChoiceOTs();
					otExtender1.extendSendingOTs();
				} catch (Exception e) {
					fail("Bad! " + e);
				}
			}
		};

		ext1.start();

		otExtender0.extendSendingOTs();
		otExtender0.extendChoiceOTs();

		otExtender0.extendSendingOTs();
		otExtender0.extendChoiceOTs();

		otExtender0.extendSendingOTs();
		otExtender0.extendChoiceOTs();

		ext1.join();

		verifyPrecomputedBlocks(otExtender1.otClientForExtension, otExtender0.otClientForExtension);

		verifyPrecomputedBlocks(otExtender1.otConsumer, otExtender0.otConsumer);
	}


	/**
	 * Test a complete cycle of send and receive, including initialization (using less than one block of precomputed OT).
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testSendAndReceiveSmall() throws IOException, InterruptedException {
		final BitMatrix x0 = new BitMatrix(m, k);
		x0.fillRandom(rand);
		final BitMatrix x1 = new BitMatrix(m, k);
		x1.fillRandom(rand);

		BitMatrix choices = new BitMatrix(k);
		choices.fillRandom(rand);

		// We need a total of four threads: one for each client and server.
		// the main thread will be client0.

		// Start client1
		Thread client1 = new Thread("Client 1") {
			public void run() {
				try {
					otClient1.init();	
					// Initialize otExtender1 from the client thread, before opening a new thread of its own.
					otExtender1.init();
					new Thread(otExtender1, "Extender 1").start();

					// Extender is now running, we can perform our OT operations.
					otClient1.send(x0, x1);
				} catch (Exception e) {
					fail("This is bad: " + e);
				}
			}
		};

		client1.start();

		otClient0.init(); // In main client0 thread
		otExtender0.init(); // In main client0 thread.
		new Thread(otExtender0, "Extender 0").start();

		BitMatrix results = otClient0.receive(choices);

		client1.join();

		for (int i = 0; i < results.getNumRows(); ++i) {
			boolean choice = choices.getBit(i) == 0;
			for (int j = 0; j < results.getNumCols(); ++j) {
				assertEquals("(Row: " + i + "; Col: " + j+")",choice ? x0.getBit(j, i) : x1.getBit(j, i), results.getBit(j, i));
			}
		}
	}
	
	/**
	 * Test a complete cycle of send and receive, including initialization (using more than one block of precomputed OT).
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testSendAndReceiveLarge() throws IOException, InterruptedException {
		final BitMatrix x0 = new BitMatrix(m, m-k+1);
		x0.fillRandom(rand);
		final BitMatrix x1 = new BitMatrix(m, m-k+1);
		x1.fillRandom(rand);

		BitMatrix choices = new BitMatrix(m-k+1);
		choices.fillRandom(rand);

		// We need a total of four threads: one for each client and server.
		// the main thread will be client0.

		// Start client1
		Thread client1 = new Thread("Client 1") {
			public void run() {
				try {
					otClient1.init();	
					// Initialize otExtender1 from the client thread, before opening a new thread of its own.
					otExtender1.init();
					new Thread(otExtender1, "Extender 1").start();

					// Extender is now running, we can perform our OT operations.
					otClient1.send(x0, x1);
				} catch (Exception e) {
					fail("This is bad: " + e);
				}
			}
		};

		client1.start();

		otClient0.init(); // In main client0 thread
		otExtender0.init(); // In main client0 thread.
		new Thread(otExtender0, "Extender 0").start();

		BitMatrix results = otClient0.receive(choices);

		client1.join();

		for (int i = 0; i < results.getNumRows(); ++i) {
			boolean choice = choices.getBit(i) == 0;
			for (int j = 0; j < results.getNumCols(); ++j) {
				assertEquals("(Row: " + i + "; Col: " + j+")",choice ? x0.getBit(j, i) : x1.getBit(j, i), results.getBit(j, i));
			}
		}
	}
	
	/**
	 * Test a complete cycle of send and receive, including initialization (using less than one block of precomputed OT).
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testSendAndReceiveMultipleSmall() throws IOException, InterruptedException {
		final BitMatrix x0 = new BitMatrix(m, m-k);
		x0.fillRandom(rand);
		final BitMatrix x1 = new BitMatrix(m, m-k);
		x1.fillRandom(rand);
		
		final int num = 100;

		BitMatrix choices = new BitMatrix(m-k);
		choices.fillRandom(rand);

		// We need a total of four threads: one for each client and server.
		// the main thread will be client0.

		// Start client1
		Thread client1 = new Thread("Client 1") {
			public void run() {
				try {
					otClient1.init();	
					// Initialize otExtender1 from the client thread, before opening a new thread of its own.
					otExtender1.init();
					new Thread(otExtender1, "Extender 1").start();

					// Extender is now running, we can perform our OT operations.
					for (int i = 0; i < num; ++i)
						otClient1.send(x0, x1);
				} catch (Exception e) {
					fail("This is bad: " + e);
				}
			}
		};

		client1.start();

		otClient0.init(); // In main client0 thread
		otExtender0.init(); // In main client0 thread.
		new Thread(otExtender0, "Extender 0").start();

		
		BitMatrix[] results = new BitMatrix[num];
		
		for (int n = 0; n < num; ++n) 
			results[n] = otClient0.receive(choices);

		client1.join();

		for (int n = 0; n < num; ++n) {
			for (int i = 0; i < results[n].getNumRows(); ++i) {
				boolean choice = choices.getBit(i) == 0;
				for (int j = 0; j < results[n].getNumCols(); ++j) {
					assertEquals("(Row: " + i + "; Col: " + j+")",choice ? x0.getBit(j, i) : x1.getBit(j, i), results[n].getBit(j, i));
				}
			}
		}
	}
}
