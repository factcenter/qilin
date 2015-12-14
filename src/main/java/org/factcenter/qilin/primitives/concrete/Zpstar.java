package org.factcenter.qilin.primitives.concrete;

import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.primitives.Group;
import org.factcenter.qilin.util.ByteEncoder;
import org.factcenter.qilin.util.IntegerUtils;
import org.factcenter.qilin.util.StreamEncoder;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;


/**
 * Multiplicative group of integers mod p (p must be a prime) 
 * @author talm
 *
 */
public class Zpstar implements Group<BigInteger>, StreamEncoder<BigInteger>, ByteEncoder<BigInteger> {
	BigInteger p;
	
	/**
	 * Construct the multiplicative group of integers mod p.
	 * 
	 * @param p must be a prime (this is not verified by the code)
	 */
	public Zpstar(BigInteger p) {
		this.p = p;
	}

	/**
	 * Group "add" operation is multiplication 
	 */
	@Override
	public BigInteger add(BigInteger el, BigInteger el2) {
		return el.multiply(el2).mod(p);
	}

	/**
	 * Group "multiply" operation is modular exponentiation 
	 */
	@Override
	public BigInteger multiply(BigInteger g, BigInteger integer) {
		return g.modPow(integer, p);
	}

	@Override
	public BigInteger negate(BigInteger g) {
		return g.modInverse(p);
	}

	@Override
	public BigInteger sample(Random rand) {
		// Return a random integer between 1 and p-1.
		return IntegerUtils.getRandomInteger(p.subtract(BigInteger.ONE), rand).add(BigInteger.ONE);
	}

	@Override
	public BigInteger orderUpperBound() {
		return p.subtract(BigInteger.ONE);
	}

	@Override
	public BigInteger injectiveEncode(byte[] msg, Random rand) throws UnsupportedOperationException {
		if (msg.length > getInjectiveEncodeMsgLength()) {
            byte[] tmp = new byte[getInjectiveEncodeMsgLength()];
            System.arraycopy(msg, 0, tmp, 0, tmp.length);
            msg = tmp;
        }

		return new BigInteger(1, msg);
	}

	@Override
	public byte[] injectiveDecode(BigInteger encodedMsg) throws UnsupportedOperationException {
		byte[] decode = encodedMsg.toByteArray();

        int off = 0;

		if (decode.length != getInjectiveEncodeMsgLength()) {
            byte[] tmp = new byte[getInjectiveEncodeMsgLength()];
            int offs = (decode[0] == 0) ? 1 : 0;
            System.arraycopy(decode, offs, tmp, 0, Math.min(decode.length - offs, tmp.length));
            decode = tmp;
		}
		return decode;
	}

	@Override
	public int getInjectiveEncodeMsgLength() throws UnsupportedOperationException {
		return (p.bitLength() - 1)/ 8;
	}

	@Override
	public boolean contains(BigInteger g) {
		return (g.signum() > 0) && (g.compareTo(p) < 0);
	}

	@Override
	public BigInteger zero() {
		return BigInteger.ONE;
	}
	@Override
	public BigInteger decode(SendableInput in) throws IOException {
		return in.readObject(BigInteger.class);
	}

	@Override
	public void encode(BigInteger g, SendableOutput out) throws IOException {
		out.writeObject(g);
	}

	@Override
	public BigInteger decode(byte[] input) {
		return new BigInteger(input);
	}

	/**
	 * Returns a random integer chosen uniformly
	 * from 0 to 2^{n-1} mod p (where n is the bit length of p's
	 * two's-complement representation); when p is large enough,
	 * this is statistically close to a uniform group member.
	 */
	@Override
	public BigInteger denseDecode(byte[] input) {
		BigInteger rand = new BigInteger(1, input);
		return rand.mod(p);
	}

	@Override
	public byte[] encode(BigInteger input) {
		return input.toByteArray();
	}

	@Override
	public int getMinLength() {
		// The number of bytes needed is the bitlength of the group
		// order (or a multiple of the order) divided by 8, rounded up. 
		int bitLen = p.bitLength();
		int byteLen = bitLen / 8 + (bitLen % 8 == 0 ? 0 : 1);
		return byteLen;
	}
}
