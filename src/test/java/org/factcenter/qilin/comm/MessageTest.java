package org.factcenter.qilin.comm;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;

public class MessageTest {
	
	/**
	 * Test a write of size less than the buffer 
	 */
	@Test
	public void testSmallReadWrite() throws IOException {
		Random rand = new Random(); 
		Message msg = new Message();
		
		OutputStream out = msg.getOutputStream();
		
		byte[] tst = new byte[Message.BUF_LEN];
		byte[] tst2 =  new byte[Message.BUF_LEN];
		
		rand.nextBytes(tst);
		
		out.write(tst, 0, Message.BUF_LEN - 10);

		assertEquals("Message should have length "+(Message.BUF_LEN - 10), 
						Message.BUF_LEN - 10, msg.length);
		InputStream in = msg.getInputStream();
		
		in.read(tst2, 0, Message.BUF_LEN - 10);
		System.arraycopy(tst, Message.BUF_LEN - 10, tst2, Message.BUF_LEN - 10, 
				10);
		
		assertArrayEquals(tst2, tst);
		assertEquals("Message should have length 0", 0, msg.length);
		assertTrue("Input stream should have ended", in.read() == -1);
	}
	
	/**
	 * Test a write of exactly the buffer 
	 */
	@Test
	public void testExactReadWrite() throws IOException {
		Random rand = new Random(); 
		Message msg = new Message();
		
		OutputStream out = msg.getOutputStream();
		
		byte[] tst = new byte[Message.BUF_LEN];
		byte[] tst2 =  new byte[Message.BUF_LEN];
		
		rand.nextBytes(tst);
		
		out.write(tst);
		assertEquals("Message should have length "+tst.length, tst.length, msg.length);
		InputStream in = msg.getInputStream();
		
		in.read(tst2);
		
		assertArrayEquals(tst2, tst);
		assertEquals("Message should have length 0", 0, msg.length);
		assertTrue("Input stream should have ended", in.read() == -1);
	}

	/**
	 * Test a write of more than the buffer 
	 */
	@Test
	public void testBigReadWrite() throws IOException {
		Random rand = new Random(); 
		Message msg = new Message();
		
		OutputStream out = msg.getOutputStream();
		
		byte[] tst = new byte[Message.BUF_LEN * 5 + 7];
		byte[] tst2 =  new byte[Message.BUF_LEN * 5 + 7];
		
		rand.nextBytes(tst);
		
		out.write(tst);
		assertEquals("Message should have length "+tst.length, tst.length, msg.length);		
		InputStream in = msg.getInputStream();
		
		in.read(tst2);
		
		assertArrayEquals(tst2, tst);
		assertTrue("Input stream should have ended", in.read() == -1);
	}
	
	/**
	 * Test a write of exact multiple of the buffer 
	 */
	@Test
	public void testExactMultipleReadWrite() throws IOException {
		Random rand = new Random(); 
		Message msg = new Message();
		
		OutputStream out = msg.getOutputStream();
		
		byte[] tst = new byte[Message.BUF_LEN * 5];
		byte[] tst2 =  new byte[Message.BUF_LEN * 5];
		
		rand.nextBytes(tst);
		
		out.write(tst);
		assertEquals("Message should have length "+tst.length, tst.length, msg.length);		
		InputStream in = msg.getInputStream();
		
		in.read(tst2);
		
		assertArrayEquals(tst2, tst);
		assertTrue("Input stream should have ended", in.read() == -1);
	}

	/**
	 * Test reads that are smaller than the write and cross buffer boundaries 
	 */
	@Test
	public void testSplitReadWrite() throws IOException {
		Random rand = new Random(); 
		Message msg = new Message();
		
		OutputStream out = msg.getOutputStream();
		
		byte[] tst = new byte[Message.BUF_LEN * 5];
		byte[] tst2 =  new byte[Message.BUF_LEN * 5];
		
		rand.nextBytes(tst);
		
		out.write(tst);
		assertEquals("Message should have length "+tst.length, tst.length, msg.length);		
		InputStream in = msg.getInputStream();
		
		int pos = 0;
		
		do {
			int tmplen = rand.nextInt(tst2.length - pos) + 1;
			int n = in.read(tst2, pos, tmplen);
			assertTrue(n > 0);
			pos += n;
		} while (pos < tst2.length);
		assertArrayEquals(tst2, tst);
		assertTrue("Input stream should have ended", in.read() == -1);
	}

	/**
	 * Test buffer write; write two buffers, then read out from an inputstream.
	 */
	@Test
	public void testBufferWrite() throws IOException {
		Random rand = new Random(); 
		Message msg = new Message();

		ByteBuffer buf = ByteBuffer.allocate(Message.BUF_LEN);
		
		byte[] tst = new byte[Message.BUF_LEN];
		byte[] tst2 =  new byte[Message.BUF_LEN];
		
		rand.nextBytes(tst);
		
		int tmplen = rand.nextInt(Message.BUF_LEN - 1) + 1;
		
		buf.put(tst, 0, tmplen);
		msg.writeBuf(buf);
		assertEquals(tmplen, msg.length());
		buf = ByteBuffer.allocate(Message.BUF_LEN);
		buf.put(tst, tmplen, tst.length - tmplen);
		msg.writeBuf(buf);
		assertEquals("Message should have length "+tst.length, tst.length, msg.length);		
		InputStream in = msg.getInputStream();
		
		int pos = 0;
		
		do {
			int n = in.read(tst2, pos, tst2.length - pos);
			assertTrue("Should be able to read at least one byte", n > 0);
			pos += n;
		} while (pos < tst2.length);
		assertArrayEquals(tst2, tst);
		assertTrue("Input stream should have ended", in.read() == -1);
	}

	/**
	 * Test buffer read; write into an outputstream, then read out as buffers.
	 */
	@Test
	public void testBufferRead() throws IOException {
		Random rand = new Random(); 
		Message msg = new Message();

		byte[] tst = new byte[Message.BUF_LEN * 2 + 7];
		byte[] tst2 = new byte[Message.BUF_LEN * 2 + 7];

		rand.nextBytes(tst);

		OutputStream out = msg.getOutputStream();

		int tmplen = rand.nextInt(Message.BUF_LEN - 1) + 1;
		out.write(tst, 0, tmplen);
		out.write(tst, tmplen, tst.length - tmplen);
		assertEquals("Message should have length "+tst.length, tst.length, msg.length);
		ByteBuffer buf;
		
		int pos = 0;
		int prevLen = msg.length();
		for (buf = msg.readBuf(); buf != null; buf = msg.readBuf()) {
			int n = buf.remaining();
			assertTrue("Should be able to read at least one byte", n > 0);
			assertEquals(prevLen - msg.length(), n);
			prevLen = msg.length();
			buf.get(tst2, pos, n);
			pos += n;
		}
		assertArrayEquals(tst2, tst);
	}
	
	/**
	 * Test message write; create a message with three buffers, write it into another message, then read out from an inputstream.
	 */
	@Test
	public void testMessageWrite() throws IOException {
		Random rand = new Random();

		Message msg = new Message();
		Message dstMsg = new Message();

		final int BUFNUM = 2;
		
		int maxlen = (BUFNUM - 1) * Message.BUF_LEN + rand.nextInt(Message.BUF_LEN - 1) + 1;
		
		
		byte[] tst = new byte[maxlen];
		rand.nextBytes(tst);

		int pos = 0;
		
		while (pos < maxlen) {
			int writelen = rand.nextInt(Message.BUF_LEN - 1) + 1;
			if (writelen > maxlen - pos)
				writelen = maxlen - pos;
			
			ByteBuffer buf = ByteBuffer.allocate(Message.BUF_LEN);
			buf.put(tst, pos, writelen);
			msg.writeBuf(buf);
			pos += writelen;
			assertEquals("Message should have length "+pos, pos, msg.length);
		}
		
		int partlen = rand.nextInt(maxlen - 1) + 1;
		int wrote = dstMsg.write(msg, partlen);
		
		assertEquals("" + partlen + " bytes of Message should have been written", partlen, wrote);
		assertEquals("Message should have length "+partlen, partlen, dstMsg.length);
		
		InputStream in = dstMsg.getInputStream();
		
		pos = 0;
		
		byte[] tst2 = new byte[tst.length];
		pos = 0;
		do {
			int n = in.read(tst2, pos, partlen - pos);
			assertTrue("Should be able to read at least one byte", n > 0);
			pos += n;
		} while (pos < partlen);
		System.arraycopy(tst, partlen, tst2, partlen, tst.length - partlen);
		assertArrayEquals(tst2, tst);
		assertTrue("Input stream should have ended", in.read() == -1);
	}
	
}
