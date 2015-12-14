package org.factcenter.qilin.primitives.concrete;

import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.factcenter.qilin.primitives.generic.BlockCipherPRG;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertArrayEquals;

/**
 * Test only the AES implementation by comparing to manually computed BouncyCastle imp.
 */
public class AESBlockCipherPRGTest {
    BlockCipherPRG prg = new BlockCipherPRG();

    AESFastEngine aes = new AESFastEngine();


    void test(byte[] key, int keylen, int blocklen, long ctr) {
        byte[] ctrBytes = new byte[blocklen];
        long tmp = ctr;
        for (int i = 0; i < 8; ++i) {
            ctrBytes[i] = (byte) (tmp & 0xff);
            tmp >>>= 8;
        }

        byte[] testkey = key;
        if (key.length != keylen) {
            testkey = new byte[keylen];
            System.arraycopy(key, 0, testkey, 0, Math.min(keylen, key.length));
        }
        aes.init(true,new KeyParameter(testkey));
        prg.setKey(key);

        byte[] prgBytes = new byte[blocklen];
        for (long i = 0; i < ctr + 1; ++i)
            prg.getPRGBytes(prgBytes, 0, prgBytes.length);

        byte[] aesBytes = new byte[blocklen];
        aes.processBlock(ctrBytes, 0, aesBytes, 0);


        assertArrayEquals("Block difference at ctr=" + ctr, aesBytes, prgBytes);
    }


    void test2(byte[] key, int keylen, int blocklen, long ctr) throws Exception {
        byte[] prgBytes = new byte[blocklen];
        byte[] prg2Bytes = new byte[blocklen];

        prg.setKey(key);
        for (long i = 0; i < ctr + 1; ++i) {
            prg.getPRGBytes(prgBytes, 0, prgBytes.length);
        }

        SecretKey skey = new SecretKeySpec(key, "AES");
        prg.setKey(skey);
        for (long i = 0; i < ctr + 1; ++i) {
            prg.getPRGBytes(prg2Bytes, 0, prg2Bytes.length);
        }

        assertArrayEquals("Block difference at ctr=" + ctr, prgBytes, prg2Bytes);
    }

    @Test
    public void AESFullKeyCtr0() throws Exception {

        byte[] key = {1, 2, 3, 4, 5, 6, 7, 8, 10, 20, 30, 40, 50, 60, 70, 80};

        test(key, 16, 16, 0);
    }


    @Test
    public void AESFullKeyCtr10() throws Exception {

        byte[] key = {1, 2, 3, 4, 5, 6, 7, 8, 10, 20, 30, 40, 50, 60, 70, 80};

        test(key, 16, 16, 10);
    }

    @Test
    public void AESPartialKeyCtr0() throws Exception {

        byte[] key = {1, 2, 3, 4, 5, 6, 7, 8, 10, 20, 30, 40, 50, 60};

        test(key, 16, 16, 0);
    }

    @Test
    public void AESPartialKeyCtr10() throws Exception {

        byte[] key = {1, 2, 3, 4, 5, 6, 7, 8, 10, 20, 30, 40, 50, 60};

        test(key, 16, 16, 10);
    }

    @Test
    public void AESFullSecretKeyCtr10() throws Exception {

        byte[] key = {1, 2, 3, 4, 5, 6, 7, 8, 10, 20, 30, 40, 50, 60, 70, 80};

        test2(key, 16, 16, 10);
    }
}
