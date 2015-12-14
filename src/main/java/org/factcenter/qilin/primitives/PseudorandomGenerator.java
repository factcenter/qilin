package org.factcenter.qilin.primitives;

import javax.crypto.SecretKey;
import java.security.InvalidKeyException;

/**
 * A pseudorandom generator.
 * The interface is mostly copied from SCAPI for compatibility
  */
public interface PseudorandomGenerator {
    /**
     * Sets the secret key for this prg.
     * If the key is too short, it is zero-padded at the end.
     * The key can be changed at any time.
     * @param secretKey secret key
     */
    public void setKey(byte[] secretKey);

    /**
     * For compatibility with SCAPI
     * @param secretKey
     * @throws InvalidKeyException
     */
    public void setKey(SecretKey secretKey) throws InvalidKeyException;

    /**
     * An object trying to use an instance of prg needs to check if it has already been initialized with a key.
     * @return true if the object was initialized by calling the function setKey.
     */
    public boolean isKeySet();

    /**
     * @return the algorithm name. For example - AES
     */
    public String getAlgorithmName();

    /**
     * Streams the prg bytes.
     * @param outBytes - output bytes. The result of streaming the bytes.
     * @param outOffset - output offset
     * @param outlen - the required output length
     */
    public void getPRGBytes(byte[] outBytes, int outOffset, int outlen);

    /**
     * Reset the PRG (without changing the key).
     * This will bring the internal state to the same
     * point as calling setKey with the currently set key.
     */
    public void reset();

    /**
     * Generates a secret key and uses it to initialize this prg object.
     */
    public void generateKey();

}
