package org.factcenter.qilin.primitives.generic;

import org.factcenter.qilin.primitives.PseudorandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * An implementation of the PRG from a block cipher
 */
public class BlockCipherPRG implements PseudorandomGenerator {
    final Logger logger = LoggerFactory.getLogger(getClass());

    public final static  String DEFAULT_CIPHER = "AES/ECB/NoPadding";

    /**
     * Add code to check unsafe access to class.
     */
    public final static boolean CHECK_THREAD_SAFETY = false;

    /**
     * Default key length in bytes.
     */
    public final static int DEFAULT_KEYLEN = 16;

    /**
     * The underlying block cipher.
     */
    Cipher cipher;

    /**
     * The algorithm portion of the cipher name (in format compatible with {@link SecretKeySpec}).
     */
    String cipherAlg;

    /**
     * Key length in bits.
     */
    int keyLen;

    boolean keySet;

    /**
     * The counter we encrypt to get the PRG bytes.
     */
    byte[] ctr;

    ByteBuffer ctrBuffer;
    ByteBuffer currentBytes;

    /**
     * Increment the counter.
     */
    private void incCounter() {
        for (int i = 0; i < ctr.length; ++i) {
            if (++ctr[i] != 0) {
                break;
            }
        }
    }

    private void createCipher(String cipherName, int keyLen)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException
    {
        String[] nameParts = cipherName.split("/", 2);
        cipherAlg = nameParts[0];
        cipher = Cipher.getInstance(cipherName);
        this.keyLen = keyLen;
        keySet = false;
        if (cipher.getBlockSize() == 0) {
            // This is a stream cipher!
            throw new RuntimeException(getClass().getName() + " requires a block cipher (not "
                    + cipher.getAlgorithm() +"!");
        }
        ctr = new byte[cipher.getBlockSize()];
        ctrBuffer = ByteBuffer.wrap(ctr);
        ctrBuffer.mark();
        currentBytes = ByteBuffer.allocate(ctr.length);
        currentBytes.flip();
        // Test key length
        byte[] empty = new byte[keyLen];
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(empty, cipherAlg));
    }

    public BlockCipherPRG(String cipherName, int keyLen)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        createCipher(cipherName, keyLen);
    }

    public BlockCipherPRG() {
        try {
            createCipher(DEFAULT_CIPHER, DEFAULT_KEYLEN);
        } catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException e) {
            logger.error("Should never happen! Cipher " + DEFAULT_CIPHER + " must be supported!", e);
        }
    }

    @Override
    public void setKey(byte[] secretKey)  {
        // Zero the counter and buffers
        reset();

        if (secretKey.length < keyLen) {
            byte[] tmp = new byte[keyLen];
            System.arraycopy(secretKey, 0, tmp, 0, secretKey.length);
            secretKey = tmp;
        }
        SecretKey key = new SecretKeySpec(secretKey, 0, keyLen, cipherAlg);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            logger.error("Should never happen! Cipher "
                    + cipher.getAlgorithm() + " should work with keylength " + keyLen + "!", e);
            throw new RuntimeException("Unexpected error setting key", e);
        }
        keySet = true;
    }

    @Override
    public void setKey(SecretKey secretKey) throws InvalidKeyException {
        setKey(secretKey.getEncoded());
    }

    @Override
    public boolean isKeySet() {
        return keySet;
    }

    @Override
    public String getAlgorithmName() {
        return cipher.getAlgorithm();
    }

    /**
     * Store thread ID of first thread to access class
     */
    long threadId = -1;
    @Override
    public void getPRGBytes(byte[] outBytes, int outOffset, int outlen) {
        if (CHECK_THREAD_SAFETY) {
            long currentThreadId = Thread.currentThread().getId();
            if (threadId == -1)
                threadId = currentThreadId;
            else if (threadId != currentThreadId) {
                logger.error("Multi-thread access to unsafe class! (first from id {}, then {})", threadId, currentThreadId);
            }
        }

        while (outlen > 0) {
            if (!currentBytes.hasRemaining()) {
                currentBytes.clear();
                try {
                    cipher.doFinal(ctrBuffer, currentBytes);
                    ctrBuffer.reset();
                    incCounter();
                    currentBytes.flip();
                } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
                    logger.error("Cipher " + cipher.getAlgorithm() + " doesn't match its stated sizes!", e);
                }
            }
            int readLen = Math.min(outlen, currentBytes.remaining());

            currentBytes.get(outBytes, outOffset, readLen);
            outlen -= readLen;
        }
    }

    @Override
    public void reset() {
        for (int i = 0; i < ctr.length; ++i)
            ctr[i] = 0;
        ctrBuffer.clear().mark();
        currentBytes.clear().flip();
    }

    @Override
    public void generateKey() {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, KeyGenerator.getInstance(cipherAlg).generateKey());
        } catch (NoSuchAlgorithmException|InvalidKeyException e) {
            logger.error("Couldn't generate key for " + cipherAlg + "!", e);
            throw new RuntimeException("Couldn't generate key!", e);
        }
    }

}
