package org.factcenter.qilin.util;

import org.factcenter.qilin.comm.Message;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;

import javax.lang.model.type.NullType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;


/**
 * A utility class for serializing/deserializing various primitive types. 
 * @author talm
 *
 */
public class EncodingUtils {
	
	/**
	 * Write an integer to a byte array, MSB first.
	 * @param n
	 * @param out
	 * @param pos the index in the bytearray at which encoding will begin.
	 * @return number of bytes written
	 */
	public static int encode(int n, byte[] out, int pos) {
		// Write length of encoding as 4 bytes, MSB first.
		for (int i = 0; i < 4; ++i) {
			out[pos + i] = ((byte) ((n >>> 24) & 0xff));
			n <<= 8;
		}
		return 4;
	}
	
	
	/**
	 * Write an integer to a ByteBuffer, MSB first.
	 * @param n
	 * @param out
	 * @return number of bytes written
	 */
	public static int encode(int n, ByteBuffer out) {
		// Write length of encoding as 4 bytes, MSB first.
		for (int i = 0; i < 4; ++i) {
			out.put((byte) ((n >>> 24) & 0xff));
			n <<= 8;
		}
		return 4;
	}
	
	/**
	 * Write an integer to an OutputStream, MSB first.
	 * @param n
	 * @param out
	 * @return number of bytes written
	 * @throws IOException
	 */
	public static int encode(int n, OutputStream out) throws IOException {
		// Write length of encoding as 4 bytes, MSB first.
		for (int i = 0; i < 4; ++i) {
			out.write((n >>> 24) & 0xff);
			n <<= 8;
		}
		return 4;
	}


	/**
	 * Write an integer to a Message, MSB first.
	 * @param n
	 * @param out
	 * @return number of bytes written
	 */
	public static int encode(int n, Message out) {
		// Write length of encoding as 4 bytes, MSB first.
		for (int i = 0; i < 4; ++i) {
			out.write((byte) ((n >>> 24) & 0xff));
			n <<= 8;
		}
		return 4;
	}
	
	/**
	 * Encode a bytearray by encoding its length and then the bytes themselves.
	 * @param arr
	 * @param out
	 * @return number of bytes written
	 * @throws IOException
	 */
	public static int encode(byte[] arr, OutputStream out) throws IOException {
		int ctr = 0;
		int len = arr.length;
		ctr += encode(len, out);
		out.write(arr); ctr += len;
		return ctr;
	}

	/**
	 * Encode a bytearray by encoding its length and then the bytes themselves.
	 * @param arr
	 * @param out
	 * @return number of bytes written
	 */
	public static int encode(byte[] arr, byte[] out, int pos) {
		pos += encode(arr.length, out, pos);
		System.arraycopy(arr, 0, out, pos, arr.length);
		pos += arr.length;
		return pos;
	}

	
	/**
	 * Encode a BigInteger by encoding its byte array representation. 
	 * @param n
	 * @param out
	 * @return number of bytes written to out
	 * @throws IOException
	 */
	public static int encode(BigInteger n, OutputStream out) throws IOException {
		return encode(n.toByteArray(), out);
	}
	
	
	/**
	 * decode  an integer from a byte array, in MSB first order.
	 * @param in
	 * @param pos the index in the bytearray at which decoding will begin.
	 * @return decoded integer
	 */
	public static int decodeInt(byte[] in, int pos) {
		int n = 0;
		// Read the length of the encoding, MSB first 
		for (int i = 0; i < 4; ++i) {
			n <<= 8;
			n |= ((int) in[pos + i]) & 0xff;
		}
		return n;
	}
	
	/**
	 * decode  an integer from a buffer, in MSB first order.
	 * @param in
	 * @return decoded integer
	 */
	public static int decodeInt(ByteBuffer in) {
		int n = 0;
		// Read the length of the encoding, MSB first 
		for (int i = 0; i < 4; ++i) {
			n <<= 8;
			byte inbyte = in.get();
			n |= ((int) inbyte) & 0xff;
		}
		return n;
	}
	
	public static int decodeInt(InputStream in) throws IOException {
		int n = 0;
		// Read the length of the encoding, MSB first 
		for (int i = 0; i < 4; ++i) {
			n <<= 8;
			int inbyte = in.read();
			if (inbyte < 0) {
				IOException ioe = new IOException("Input stream too short to decode int");
				ioe.fillInStackTrace();
				throw ioe;
			}
			
			n |= inbyte;
		}
		return n;
	}

	/**
	 * decode  an integer from a Message, in MSB first order.
	 * @param in
	 * @return decoded integer
	 */
	public static int decodeInt(Message in) {
		int n = 0;
		// Read the length of the encoding, MSB first 
		for (int i = 0; i < 4; ++i) {
			n <<= 8;
			byte inbyte = (byte) in.read();
			n |= ((int) inbyte) & 0xff;
		}
		return n;
	}

	
	public static byte[] decodeByteArray(InputStream in) throws IOException {
		int len = decodeInt(in);
		
		byte[] arr = new byte[len];
		int offs = 0;
		do {
			int n = in.read(arr, offs, len - offs);
			if (n < 0) {
				IOException ioe = new IOException("Input stream too short to decode byte array");
				ioe.fillInStackTrace();
				throw ioe;
			}
			offs += n;
		} while (offs < len);
		return arr;
	}


	/**
	 * decode  a byte array  from a byte array, where the length
	 * is given first in MSB first order.
	 * @param in
	 * @param pos the index in the bytearray at which decoding will begin.
	 * @return decoded array.
	 */
	public static byte[] decodeByteArray(byte[] in, int pos) {
	
		int n = decodeInt(in, pos);
		assert n >= 0;
		byte[] ret = new byte[n];
		System.arraycopy(in, pos + 4, ret, 0, n);
		return ret;
	}

	
	public static BigInteger decodeBigInteger(InputStream in) throws IOException {
		byte[] encoded = decodeByteArray(in);
		return new BigInteger(encoded);
	}

	public static class ByteArrayEncoder implements StreamEncoder<byte[]>, ByteEncoder<byte[]> {
		int minLength;
		
		public ByteArrayEncoder(int minLength) {
			this.minLength = minLength;
		}
		
//		@Override
//		public byte[] decode(InputStream in) throws IOException {
//			return EncodingUtils.decodeByteArray(in);
//		}
//
//		@Override
//		public int encode(byte[] input, OutputStream out) throws IOException {
//			return EncodingUtils.encode(input, out);
//		}

		@Override
		public byte[] decode(byte[] input) {
			return input;
		}

		@Override
		public byte[] denseDecode(byte[] input) {
			return input;
		}

		@Override
		public byte[] encode(byte[] input) {
			return input;
		}

		@Override
		public int getMinLength() {
			return minLength;
		}

		@Override
		public void encode(byte[] input, SendableOutput out) throws IOException {
			out.writeObject(input);
		}

		@Override
		public byte[] decode(SendableInput in) throws IOException {
			return in.readObject(byte[].class);
		}
	}
	

	public static class StringEncoder implements StreamEncoder<String>, ByteEncoder<String> {
		int minLength;
		
		public StringEncoder(int minLength) {
			this.minLength = minLength;
		}
		
//		@Override
//		public String decode(InputStream in) throws IOException {
//			return new String(EncodingUtils.decodeByteArray(in));
//		}
//
//		@Override
//		public int encode(String input, OutputStream out) throws IOException {
//			return EncodingUtils.encode(input.getBytes(), out);
//		}

		@Override
		public String decode(byte[] input) {
			return new String(input);
		}

		@Override
		public String denseDecode(byte[] input) {
			return new String(input);
		}

		@Override
		public byte[] encode(String input) {
			return input.getBytes();
		}

		@Override
		public int getMinLength() {
			return minLength;
		}

		@Override
		public void encode(String input, SendableOutput out) throws IOException {
			out.writeUTF(input);
		}

		@Override
		public String decode(SendableInput in) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	public static class NullEncoder implements StreamEncoder<NullType>, ByteEncoder<NullType> {
//		@Override
//		public NullType decode(InputStream in) throws IOException {
//			return null;
//		}
//
//		@Override
//		public int encode(NullType input, OutputStream out) throws IOException {
//			return 0;
//		}

		@Override
		public NullType decode(byte[] input) {
			return null;
		}

		@Override
		public NullType denseDecode(byte[] input) {
			return null;
		}

		@Override
		public byte[] encode(NullType input) {
			return null;
		}

		@Override
		public int getMinLength() {
			return 0;
		}

		@Override
		public void encode(NullType input, SendableOutput out)
				throws IOException {
		}

		@Override
		public NullType decode(SendableInput in) throws IOException {
			return null;
		}
	}
}
