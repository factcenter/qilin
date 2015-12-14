package org.factcenter.qilin.primitives.concrete;

import org.factcenter.qilin.primitives.CyclicGroup;
import org.factcenter.qilin.util.ByteEncoder;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.util.IntegerUtils;
import org.factcenter.qilin.util.StreamEncoder;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;


/**
 * The additive group of integers modulo n
 * @author talm
 *
 */
public class Zn implements CyclicGroup<BigInteger>, StreamEncoder<BigInteger>, ByteEncoder<BigInteger> {
	BigInteger n;

	/**
	 * Create a new additive group of integers mod n
	 * @param n
	 */
	public Zn(BigInteger n) {
		this.n = n;
	}
	
	@Override
	public BigInteger add(BigInteger el, BigInteger el2) {
		return el.add(el2).mod(n);
	}

	@Override
	public BigInteger multiply(BigInteger g, BigInteger integer) {
		return g.multiply(integer);
	}

	@Override
	public BigInteger negate(BigInteger g) {
		return g.negate().mod(n);
	}

	@Override
	public BigInteger orderUpperBound() {
		return n;
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
		return (n.bitLength() - 1)/ 8;
	}

	@Override
	public BigInteger sample(Random rand) {
		return IntegerUtils.getRandomInteger(n, rand);
	}

	@Override
	public boolean contains(BigInteger g) {
		return (g.signum() >= 0) && (g.compareTo(n) < 0);
	}

	@Override
	public BigInteger zero() {
		return BigInteger.ZERO;
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

	@Override
	public BigInteger denseDecode(byte[] input) {
		BigInteger rand = new BigInteger(1, input);
		return rand.mod(n);
	}

	@Override
	public byte[] encode(BigInteger input) {
		return input.toByteArray();
	}

	@Override
	public int getMinLength() {
		// The number of bytes needed is the bitlength of the group
		// order (or a multiple of the order) divided by 8, rounded up. 
		int bitLen = n.bitLength();
		int byteLen = bitLen / 8 + (bitLen % 8 == 0 ? 0 : 1);
		return byteLen;
	}

	@Override
	public BigInteger getGenerator() {
		return BigInteger.ONE;
	}
}
