package org.factcenter.qilin.primitives.concrete;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.primitives.CyclicGroup;
import org.factcenter.qilin.util.ByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.util.IntegerUtils;
import org.factcenter.qilin.util.StreamEncoder;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

/**
 * An Elliptic-Curve group. Uses the <a href="http://www.bouncycastle.org/">BouncyCastle library</a> for
 * the low-level elliptic-curve operations. 
 * @author talm
 *
 */
public class ECGroup implements CyclicGroup<ECPoint>, ByteEncoder<ECPoint>, StreamEncoder<ECPoint> {
	final Logger logger = LoggerFactory.getLogger(getClass());

	ECParameterSpec curveParams;
	
	public ECGroup(ECParameterSpec curveParams) {
		this.curveParams = curveParams;
	}
	
	/**
	 * Construct an EC curve group using a named curve. 
	 * For reasonable (NIST-approved) security parameters,
	 * use "P-256".
	 * @param namedParams
	 * @throws IllegalArgumentException
	 */
	public ECGroup(String namedParams) throws IllegalArgumentException {
		this.curveParams = ECNamedCurveTable.getParameterSpec(namedParams);
		if (curveParams == null)
			throw new IllegalArgumentException("No such curve: " + namedParams);
	}

	public ECParameterSpec getCurveParams() {
		return curveParams;
	}
	
	public ECPoint getGenerator() {
		return curveParams.getG();
	}
	
	@Override
	public ECPoint add(ECPoint el, ECPoint el2) {
		return el.add(el2);
	}

	@Override
	public ECPoint multiply(ECPoint g, BigInteger integer) {
		return g.multiply(integer);
	}

	@Override
	public ECPoint negate(ECPoint g) {
		if (g.isInfinity())
			return g;
		return g.negate();
	}

	/**
	 * Return random point by multiplying base point with
	 * a random scalar (there are more efficient ways of doing this)
	 */
	@Override
	public ECPoint sample(Random rand) {
		return curveParams.getG().multiply(
				IntegerUtils.getRandomInteger(curveParams.getN(), rand));
	}

	@Override
	public BigInteger orderUpperBound() {
		return curveParams.getN();
	}

	/** 
	 * Check that y^2=x^3+Ax+B, where A and B are the curve parameters 
	 */
	@Override
	public boolean contains(ECPoint g) {
		if (g.isInfinity())
			return true;
		
		ECCurve curve = curveParams.getCurve();
		
		ECFieldElement A = curve.getA();
		ECFieldElement B = curve.getB();
		
		ECFieldElement X = g.normalize().getXCoord();
		ECFieldElement Y = g.normalize().getYCoord();
		
		ECFieldElement X3 = X.square().multiply(X);
		
		return Y.square().equals(X3.add(A.multiply(X)).add(B));
	}

	@Override
	public ECPoint zero() {
		return curveParams.getCurve().getInfinity();
	}

	public ECPoint decode(byte[] encoded) {
		return curveParams.getCurve().decodePoint(encoded);
	}
	
//	@Override
//	public ECPoint decode(InputStream in) throws IOException {
//		byte[] encoded = EncodingUtils.decodeByteArray(in);
//		return curveParams.getCurve().decodePoint(encoded);
//	}
	
	@Override
	public ECPoint decode(SendableInput in) throws IOException {
		byte[] encoded = in.readObject(byte[].class);
		return curveParams.getCurve().decodePoint(encoded);
	}

	public byte[] encode(ECPoint g) {
		return g.getEncoded(true);
	}
	
//	@Override
//	public int encode(ECPoint g, OutputStream out) throws IOException {
//		byte[] encoded = g.getEncoded();
//		return EncodingUtils.encode(encoded, out);
//	}


	@Override
	public void encode(ECPoint g, SendableOutput out) throws IOException {
		byte[] encoded = g.getEncoded(true);
		out.writeObject(encoded);
	}
	
	/**
	 * The dense decoding uses the input as a random integer
	 * and multiplies the group generator by that integer.
	 */
	@Override
	public ECPoint denseDecode(byte[] input) {
		BigInteger rand = new BigInteger(1, input);
		return getGenerator().multiply(rand);
	}

	@Override
	public int getMinLength() {
		// The number of bytes needed is the bitlength of the group
		// order (or a multiple of the order) divided by 8, rounded up. 
		int bitLen = curveParams.getN().bitLength();
		int byteLen = bitLen / 8 + (bitLen % 8 == 0 ? 0 : 1);
		return byteLen;
	}

    /**
     * Failure prob. for encoding is at most 2^-{2^LOG_LOG_ENCODING_ERROR_RATE}
     */
    public final static int LOG_LOG_ENCODING_ERROR_RATE = 6;

    /**
     * Size of random nonce (in bytes) used in injective encoding
     */
    public final static int NONCE_LENGTH = 8;

    /**
     * We encode the message using a probabilistic algorithm.
     *
     * @param msg The message to be encoded
     * @param rand
     * @return
     * @throws UnsupportedOperationException
     */
	@Override
	public ECPoint injectiveEncode(byte[] msg, Random rand) throws UnsupportedOperationException {

        final int BLOCK_LEN = getInjectiveEncodeMsgLength();
        ECCurve curve = curveParams.getCurve();
        if (!(curve instanceof ECCurve.AbstractFp)) {
            throw new UnsupportedOperationException("We currently only support prime fields for message encoding");
        }
        ECCurve.AbstractFp curveFp = (ECCurve.AbstractFp) curve;

        if (msg.length != BLOCK_LEN) {
            if (msg.length > BLOCK_LEN)
                logger.warn("Message larger than maximum length ({} bytes, field size is {} bytes), truncating", msg.length,
                        BLOCK_LEN);
            byte[] tmp = new byte[BLOCK_LEN];
            System.arraycopy(msg, 0, tmp, 0, Math.min(tmp.length, msg.length));
            msg = tmp;
        }

		// For now we use a probabilistic method.
        ECFieldElement A = curveFp.getA();
        ECFieldElement B = curveFp.getB();
        BigInteger M = BigIntegers.fromUnsignedByteArray(msg);

        ECFieldElement y = null;
		ECFieldElement x = null;
		byte[] randBytes = new byte[NONCE_LENGTH];
        for (int j = 0; j < (1 << LOG_LOG_ENCODING_ERROR_RATE); ++j) {
			rand.nextBytes(randBytes);
			x = curveFp.fromBigInteger(
					BigIntegers.fromUnsignedByteArray(randBytes)
					.shiftLeft(BLOCK_LEN * 8).or(M));

            ECFieldElement z = x.square().multiplyPlusProduct(x,A,x).add(B);
            y = z.sqrt();
            if (y != null)
                break;
        }
        if (y == null) {
            logger.error("Miracle! Didn't find mapping for msg -- occurs with prob 2^{-{}}", 1<< LOG_LOG_ENCODING_ERROR_RATE);
            return null;
        }
        return curveFp.createPoint(x.toBigInteger(), y.toBigInteger());
	}

    @Override
    public byte[] injectiveDecode(ECPoint encodedMsg) throws UnsupportedOperationException {
        byte[] decoded = new byte[getInjectiveEncodeMsgLength()];
        injectiveDecode(encodedMsg, decoded);
        return  decoded;
    }


    public void injectiveDecode(ECPoint encodedMsg, byte[] decodedMsg) throws UnsupportedOperationException {
        final int BLOCK_LEN = getInjectiveEncodeMsgLength();
        BigInteger blockMask = BigInteger.ONE
                .shiftLeft(BLOCK_LEN * 8)
                .subtract(BigInteger.ONE);
        BigInteger x = encodedMsg.normalize().getAffineXCoord().toBigInteger();
        byte[] xBytes = x.and(blockMask).toByteArray();

        if (xBytes[0] != 0) {
            System.arraycopy(xBytes, 0, decodedMsg, 0, Math.min(xBytes.length, decodedMsg.length));
        } else {
            // BigInteger returns zero-pads to prevent a negative number.
            System.arraycopy(xBytes, 1, decodedMsg, 0, Math.min(xBytes.length - 1, decodedMsg.length));
        }

    }

	@Override
	public int getInjectiveEncodeMsgLength() throws UnsupportedOperationException {
        ECCurve curve = curveParams.getCurve();
        if (!(curve instanceof ECCurve.AbstractFp)) {
            throw new UnsupportedOperationException("We currently only support prime fields for message encoding");
        }
        ECCurve.AbstractFp curveFp = (ECCurve.AbstractFp) curve;

        // TODO: Is this optimal? (we use 8 bytes for the random nonce)
        return (curveFp.getFieldSize() - 1) / 8 - 8;
	}
}
