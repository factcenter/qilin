package org.factcenter.qilin.comm;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Represents a single message. 
 * For efficiency, message data is stored in {@link ByteBuffer}s.
 *  
 * @author talm
 *
 */
public class Message {
	public final static int BUF_LEN = 4096; 
	
	/**
	 * Length of the message (in bytes).
	 */
	int length;
	
	/**
	 * Linked list of additional buffers if the message
	 * is longer than a single buffer.
	 */
	LinkedList<ByteBuffer> bufs;
	
	/**
	 * Index of buffer from to which next write operation
	 * will occur (last buffer in list). 
	 */
	ByteBuffer writeBuf;
	
	/**
	 * Buffer from which next read operation will
	 * occur (first buffer in list).
	 */
	ByteBuffer readBuf;
	
	/**
	 * index in readBuf from which next read operation will begin.
	 */
	int readPos;

	public Message() {
		bufs = new LinkedList<ByteBuffer>();
		writeBuf = readBuf = ByteBuffer.allocate(BUF_LEN);
		readPos = 0;
		writeBuf.clear();
		length = 0;
	}
	
	public void clear() {
		bufs.clear();
		writeBuf = readBuf;
		writeBuf.clear();
		readPos = 0;
		length = 0;
	}
	
	/**
	 * Write an entire buffer to the message.
	 * Note that the buffer is added to the message, not copied. The buffer should be in a "writeable" state 
	 * (i.e., as after a "compact" operation).
	 * @param buf
	 */
	public void writeBuf(ByteBuffer buf) {
		if (buf.position() == 0)
			// Don't write empty buffers.
			return;
		
		if (writeBuf.position() == 0) {
			// this can only happen if bufs is empty
			// and readBuf == writeBuf. In this case we
			// replace the existing buffer.
			readBuf = writeBuf = buf;
			length = buf.position();
		} else {
			bufs.add(buf);
			writeBuf = buf;
			length += buf.position();
		}
	}
	
	/**
	 * Read an entire buffer from the message.
	 * @return a buffer containing at least one byte, or null if the queue is empty.
	 * 			The buffer returned is ready to be read from (i.e., no need to call flip()).
	 */
	public ByteBuffer readBuf() {
		ByteBuffer buf = readBuf;
		buf.flip();
		buf.position(readPos);
		if (buf.remaining() == 0) {
			if (bufs.isEmpty()) {
				// There's nothing to read, so we won't return this buffer.
				// We can clear it just in case...
				readBuf.clear();
				readPos = 0;
				return null;
			} else {
				buf = bufs.remove();
			}
		}
		if (bufs.isEmpty()) {
			writeBuf = readBuf = ByteBuffer.allocate(BUF_LEN);
		} else {
			readBuf = bufs.remove();
		}
		readPos = 0;
		length -= buf.remaining();
		return buf;
	}
	
	/**
	 * Ensure at least one byte of space in the current write buffer
	 */
	final void ensureCapacity() {
		if (writeBuf.remaining() == 0) {
			writeBuf = ByteBuffer.allocate(BUF_LEN);
			writeBuf.clear();
			bufs.add(writeBuf);
		}
	}

	

	/**
	 * Write part of another message. The data is read from the other message. 
	 * Entire buffers can be moved between messages without being copied if
	 * this is more efficient.
	 *  
	 * 
	 * @param m The message from which data should be written
	 * @param len At most this many bytes are read from the message m and written to this message.
	 * @return the number of bytes actually written.
	 */
	public int write(Message m, int len) {
		int left = len;
		while(m.length > 0 && (m.readBuf.position() - m.readPos) <= left) {
			left -= m.readBuf.position() - m.readPos;
			ByteBuffer buf = m.readBuf();
			assert buf != null; // If length > 0, there must be at least one buffer. 
			buf.compact();
			writeBuf(buf);
		}
		if (m.length > 0) {
			// We arrive here only when the remaining buffer has more than left bytes,
			// so we cannot just transfer the entire buffer. Instead, we read left bytes
			// from it.
			if (writeBuf.remaining() < left) {
				writeBuf = ByteBuffer.allocate(BUF_LEN);
				writeBuf.clear();
				bufs.add(writeBuf);
			}
			int oldLimit = m.readBuf.position();
			m.readBuf.position(m.readPos);
			m.readBuf.limit(m.readPos + left);
			writeBuf.put(m.readBuf);
			length += left;
			m.length -= left;
			m.readPos = m.readBuf.position();
			m.readBuf.limit(m.readBuf.capacity());
			m.readBuf.position(oldLimit);

			left = 0;
		}
		return len - left;
	}
	
	
	/**
	 * Write part of a byte array.
	 * 
	 * @param b
	 * @param offs
	 * @param len
	 * @see java.io.OutputStream#write(byte[],int,int)
	 */
	public void write(byte[] b, int offs, int len) {
		do {
			ensureCapacity();
			int writelen = len;
			if (len > writeBuf.remaining())
				writelen = writeBuf.remaining();
			
			writeBuf.put(b, offs, writelen);
			len -= writelen;
			offs += writelen;
			length += writelen;
		} while (len > 0);
	}
	

	/**
	 * Write a single byte. 
	 * @param b
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) {
		ensureCapacity();
		writeBuf.put((byte) b);
		++length;
	}
	
	
	/**
	 * Skip this many bytes (without actually reading them).
	 * @param len
	 * @return number of bytes actually skipped.
	 */
	public int skip(int len) {
		return read(null, 0, len);
	}

	/**
	 * Read from the message into a byte array.
	 * @param b byte array to read into. If null, read will just skip the bytes.
	 * @param offs offset in the byte array at which writing will start
	 * @param len maximum number of bytes to read.
	 * @return the number of bytes actually read.
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int offs, int len) {
		int count = 0;

		readBuf.flip();
		readBuf.position(readPos);
		do {
			int readremaining = readBuf.remaining(); 
			if (readremaining == 0) {
				if (!bufs.isEmpty()) {
					readBuf = bufs.remove();
					readBuf.flip(); // This also sets the read position to 0 
					readremaining = readBuf.remaining();
					// Non-first buffer is guaranteed to have 
					// at least one byte (since write doesn't
					// create a new buffer until it needs to).
				} else {
					// Nothing left to read
					break;
				}
			}
			int readlen = len;
			if (readlen > readremaining) {
				readlen = readremaining;
			}
			if (b != null) {
				readBuf.get(b, offs, readlen);
				offs += readlen;
			}
			count += readlen;
			len -= readlen;
			length -= readlen;
		} while (len > 0);
		// "Unflip" the buffer. We could do this using "readBuf.compact()", 
		// but this would cause a copy for every read operation.
		readPos = readBuf.position();
		readBuf.position(readBuf.limit());
		readBuf.limit(readBuf.capacity());
		 
		if (count == 0 && len > 0)
			// simulate "end of file"
			return -1;
		return count;
	}

	/**
	 * Read a single byte from the message
	 * @return the byte read (or -1 if none are available)
	 * @see java.io.InputStream#read()
	 */
	public int read() {
		byte[] tmp = new byte[1];

		int n = read(tmp, 0, 1);
		if (n == -1)
			return -1;
		return ((int)tmp[0]) & 0xff;
	}
	
	/**
	 * Return a stream to which message contents can be written.
	 */
	public OutputStream getOutputStream()  {
		return new OutputStream() {
			
			
			@Override
			public void write(byte[] b, int offs, int len) {
				Message.this.write(b, offs, len);
			}

			@Override
			public void write(int b) {
				Message.this.write(b);
			}
		};
	}

	/**
	 * Return a stream from which message contents can be read.
	 */
	public InputStream getInputStream() {
		return new InputStream() {

			@Override
			public int read(byte[] b, int offs, int len) {
				return Message.this.read(b, offs, len);
			}

			@Override
			public int read() {
				return Message.this.read();
			}
		};
	}

	public int length() {
		return length;
	}
}
