package org.factcenter.qilin.primitives.generic;

import org.junit.Before;
import org.junit.Test;

/**
 * Test {@link BlockCipherPRG}
 */
public class BlockCipherPRGTest {
    BlockCipherPRG prg;


    @Before
    public void setup() {
        prg = new BlockCipherPRG();
    }

    @Test
    public void testPRGRuns() throws Exception {
        prg.generateKey();

        byte[] out = new byte[100];
        prg.getPRGBytes(out, 0, out.length);
    }
}
