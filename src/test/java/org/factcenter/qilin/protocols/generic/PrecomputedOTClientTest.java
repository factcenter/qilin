package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.primitives.PseudorandomGenerator;
import org.factcenter.qilin.primitives.StreamingRandomOracle;
import org.factcenter.qilin.primitives.concrete.DigestOracle;
import org.factcenter.qilin.primitives.generic.BlockCipherPRG;
import org.factcenter.qilin.util.BitMatrix;
import org.factcenter.qilin.util.Pair;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PrecomputedOTClientTest {
	Random rand;
	PseudorandomGenerator clientPrg0, clientPrg1;
	StreamingRandomOracle clientH0, clientH1;

	PrecomputedOTClient otClient0;
	PrecomputedOTClient otClient1;

	// These two are connected.
	Channel clientChannel0; 
	Channel clientChannel1;

	final int k = 80;
	final int m = 240;

	@Before
	public void setup() {
		rand = new Random(0);
		clientPrg0 = new BlockCipherPRG();
		clientH0 = new DigestOracle();
		clientPrg1 = new BlockCipherPRG();
		clientH1 = new DigestOracle();

		LocalChannelFactory lcf = new LocalChannelFactory();
		Channel[] channels = lcf.getChannelPair();
		clientChannel0 = channels[0];
		clientChannel1 = channels[1];

		otClient0 = new PrecomputedOTClient(0, 1, clientPrg0, clientH0);
		otClient1 = new PrecomputedOTClient(1, 1, clientPrg1, clientH1);
		
		otClient0.setParameters(clientChannel0, rand);
		otClient1.setParameters(clientChannel1, rand);
	}


	/**
	 * Generated a precomputed OT block pair.
	 * @param len
	 * @param num
	 */
	public static Pair<PrecomputedChoiceOTBlock, PrecomputedSendingOTBlock> generateDummyBlock(int len, int num, Random rand) {
		BitMatrix choices = new BitMatrix(num);
		choices.fillRandom(rand);

		BitMatrix x0 = new BitMatrix(len, num);
		x0.fillRandom(rand);

		BitMatrix x1 = new BitMatrix(len, num);
		x1.fillRandom(rand);

		BitMatrix results = new BitMatrix(len, num);

		for (int i = 0; i < num; ++i) {
			results.copyRow(i, (choices.getBit(i) == 0) ? x0 : x1, i); 
		}

		PrecomputedChoiceOTBlock choiceBlock = new PrecomputedChoiceOTBlock(choices, results);
		PrecomputedSendingOTBlock sendBlock = new PrecomputedSendingOTBlock(x0, x1);
		return new Pair<PrecomputedChoiceOTBlock, PrecomputedSendingOTBlock>(choiceBlock, sendBlock);
	}

	

	@Test
	public void testSendAndReceiveNoPRG() throws IOException {
		Pair<PrecomputedChoiceOTBlock, PrecomputedSendingOTBlock> block = generateDummyBlock(k, k, rand);

		otClient0.getChoiceOTBlockQueue().addOTs(block.a);
		otClient1.getSendingOTBlockQueue().addOTs(block.b);

		final BitMatrix x0 = new BitMatrix(k, k);
		x0.fillRandom(rand);
		final BitMatrix x1 = new BitMatrix(k, k);
		x1.fillRandom(rand);

		BitMatrix choices = new BitMatrix(k);
		choices.fillRandom(rand);


		new Thread() {
			@Override
			public void run() {
				try {
					otClient1.send(x0, x1);
				} catch (IOException e) {
					fail("Shouldn't ever happen:" + e);
				}
			}
		}.start();

		BitMatrix results = otClient0.receive(choices);

		for (int i = 0; i < k; ++i) {
			boolean choice = choices.getBit(i) == 0;
			for (int j = 0; j < k; ++j) {
				assertEquals("(Row: " + i + "; Col: " + j+")",choice ? x0.getBit(j, i) : x1.getBit(j, i), results.getBit(j, i));
			}
			//			BitMatrix a = results.getSubMatrix(i, 1).clone();
			//			BitMatrix b = (choices.getBit(i) == 0) ? x0.getSubMatrix(i, 1).clone() : x1.getSubMatrix(i, 1).clone();
			//
			//			assertArrayEquals("Row " + i + ": ", b.getBits(), a.getBits());
		}

	}



	@Test
	public void testSendAndReceive() throws IOException {
		Pair<PrecomputedChoiceOTBlock, PrecomputedSendingOTBlock> block = generateDummyBlock(k, k, rand);

		otClient0.getChoiceOTBlockQueue().addOTs(block.a);
		otClient1.getSendingOTBlockQueue().addOTs(block.b);

		final BitMatrix x0 = new BitMatrix(m, k);
		x0.fillRandom(rand);
		final BitMatrix x1 = new BitMatrix(m, k);
		x1.fillRandom(rand);

		BitMatrix choices = new BitMatrix(k);
		choices.fillRandom(rand);


		new Thread() {
			@Override
			public void run() {
				try {
					otClient1.send(x0, x1);
				} catch (IOException e) {
					fail("Shouldn't ever happen:" + e);
				}
			}
		}.start();

		BitMatrix results = otClient0.receive(choices);

		for (int i = 0; i < k; ++i) {
			boolean choice = choices.getBit(i) == 0;
			for (int j = 0; j < m; ++j) {
				assertEquals("(Row: " + i + "; Col: " + j+")",choice ? x0.getBit(j, i) : x1.getBit(j, i), results.getBit(j, i));
			}
			//			BitMatrix a = results.getSubMatrix(i, 1).clone();
			//			BitMatrix b = (choices.getBit(i) == 0) ? x0.getSubMatrix(i, 1).clone() : x1.getSubMatrix(i, 1).clone();
			//
			//			assertArrayEquals("Row " + i + ": ", b.getBits(), a.getBits());
		}

	}


    @Test
    public void testSendAndReceiveTwoBlocks() throws IOException {
        for (int i = 0; i < 2; ++i) {
            Pair<PrecomputedChoiceOTBlock, PrecomputedSendingOTBlock> block = generateDummyBlock(k, k, rand);

            otClient0.getChoiceOTBlockQueue().addOTs(block.a);
            otClient1.getSendingOTBlockQueue().addOTs(block.b);
        }

        int num = (int) (k * 1.5);
        int total = 0;
        while (total < k*2) {
            final BitMatrix x0 = new BitMatrix(m, num);
            x0.fillRandom(rand);
            final BitMatrix x1 = new BitMatrix(m, num);
            x1.fillRandom(rand);

            BitMatrix choices = new BitMatrix(num);
            choices.fillRandom(rand);


            new Thread() {
                @Override
                public void run() {
                    try {
                        otClient1.send(x0, x1);
                    } catch (IOException e) {
                        fail("Shouldn't ever happen:" + e);
                    }
                }
            }.start();

            BitMatrix results = otClient0.receive(choices);

            for (int i = 0; i < num; ++i) {
                boolean choice = choices.getBit(i) == 0;
                for (int j = 0; j < m; ++j) {
                    assertEquals("(Row: " + i + "; Col: " + j + ")", choice ? x0.getBit(j, i) : x1.getBit(j, i), results.getBit(j, i));
                }
                //			BitMatrix a = results.getSubMatrix(i, 1).clone();
                //			BitMatrix b = (choices.getBit(i) == 0) ? x0.getSubMatrix(i, 1).clone() : x1.getSubMatrix(i, 1).clone();
                //
                //			assertArrayEquals("Row " + i + ": ", b.getBits(), a.getBits());
            }
            total += num;
            num = k*2-total;
        }

    }
}
