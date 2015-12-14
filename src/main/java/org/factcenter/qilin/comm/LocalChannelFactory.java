package org.factcenter.qilin.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Class for handling local (in-process) connections. 
 * The class creates channel pairs that are connected to each other;
 * messages sent through one channel are received on the other.
 * @author talm
 *
 */
public class LocalChannelFactory {
	final Logger logger = LoggerFactory.getLogger(getClass());
	
	public class LocalChannel implements Channel {
		LocalChannel peerChannel;
		String name;

		private long bytesWritten = 0;
		private long bytesRead = 0;
		
		final static int BUF_SIZE = 1024;
		BlockingQueue<ByteBuffer> bufs;
		ByteBuffer readBuf;
		ByteBuffer writeBuf;
		
		/**
		 * A {@link SendableOutputStream} wrapping this Channel's {@link LocalOutputStream}.
		 */
		SendableOutputStream dataOut;
		
		/**
		 * A {@link SendableInputStream} wrapping this Channel's {@link LocalInputStream}.
		 */
		SendableInputStream dataIn;
		

		/**
		 * Last thread to read from this Channel.
		 * Used for debugging (to catch two different threads reading).
		 */
		Thread lastReadingThread = null; 
		
		
		/**
		 * Last thread to write to this Channel.
		 * Used for debugging (to catch two different threads writing).
		 */
		Thread lastWritingThread = null; 
		
		
		
		@Override
		public String toString() {
			return name;
		}
		
		public long getBytesWritten(){
			return bytesWritten;
		}
		
		public long getBytesRead(){
			return bytesRead;
		}
		
		LocalChannel(String name) {
			this.name = name;
			bufs = new LinkedBlockingQueue<ByteBuffer>();
			dataOut = new SendableOutputStream(new LocalOutputStream());
			dataIn = new SendableInputStream(new LocalInputStream());
		}
		
		void connect(LocalChannel peerChannel) {
			this.peerChannel = peerChannel;
			peerChannel.peerChannel = this;
		}


		/**
		 * Check if another thread has already read from this channel.
		 */
		void checkReadingThread() {
			if (logger.isDebugEnabled()) {
				Thread curThread = Thread.currentThread();
				if (lastReadingThread == null) {
					lastReadingThread = curThread;
				} else if (lastReadingThread != curThread) {
//					logger.debug("Read from channel {} changed threads (old={}, new={}", 
//							name, lastReadingThread, curThread);
					lastReadingThread = curThread;
				}
			}
		}
		
		/**
		 * Check if another thread has already written to this channel.
		 */
		void checkWritingThread() {
			if (logger.isDebugEnabled()) {
				Thread curThread = Thread.currentThread();
				if (lastWritingThread == null) {
					lastWritingThread = curThread;
				} else if (lastWritingThread != curThread) {
//					logger.debug("Write to channel {} changed threads (old={}, new={}", 
//							name, lastWritingThread, curThread);
					lastWritingThread = curThread;
				}
			}
		}
		
		/**
		 * Implements read from buffers.
		 * We extend InputStream to 
		 * @author talm
		 *
		 */
		class LocalInputStream extends InputStream {
			/**
			 * Read from the message into a byte array.
			 * @param b byte array to read into. If null, read will just skip the bytes.
			 * @param offs offset in the byte array at which writing will start
			 * @param len maximum number of bytes to read.
			 * @return the number of bytes actually read.
			 * @see java.io.InputStream#read(byte[], int, int)
			 */
			@Override
			public int read(byte[] b, int offs, int len) throws IOException {
				checkReadingThread();
				// Block if there's nothing at all to return
				if (readBuf == null || readBuf.remaining() == 0) {
					try {
						readBuf = bufs.take();
					} catch (InterruptedException e) {
						throw new IOException("Unexpected interruption: " + e.getMessage());
					}
				}

				int readlen = len;
				if (len > readBuf.remaining()) {
					readlen = readBuf.remaining();
				}
				if (b != null) {
					readBuf.get(b, offs, readlen);
					offs += readlen;
				}
				bytesRead+=readlen;
				return readlen;
			}
			
			
			@Override
			public int read() throws IOException {
				checkReadingThread();
				// Block if there's nothing at all to return
				if (readBuf == null || readBuf.remaining() == 0) {
					try {
						readBuf = bufs.take();
						assert(readBuf.remaining() > 0);
					} catch (InterruptedException e) {
						throw new IOException("Unexpected interruption: " + e.getMessage());
					}
				}
				
				bytesRead++;
				byte b = readBuf.get();
				return ((int) b) & 0xff; 
			}
		}
		
		class LocalOutputStream extends OutputStream {
			@Override
			public void write(byte[] buf, int offs, int len) throws IOException {
				checkWritingThread();
				while (len > 0) {
					if (writeBuf == null || writeBuf.remaining() == 0)
						flush();

					int writeLen = len;
					if (writeLen > writeBuf.remaining())
						writeLen = writeBuf.remaining();
					
					writeBuf.put(buf, offs, writeLen);
					offs += writeLen;
					len -= writeLen;
					bytesWritten+=writeLen;
				}
			}
			
			@Override
			public void write(int arg0) throws IOException {
				checkWritingThread();
				if (writeBuf == null || writeBuf.remaining() == 0)
					flush();
				writeBuf.put((byte) arg0);
				bytesWritten++;
			}
			
			@Override
			public void flush() throws IOException {
				if (writeBuf != null && writeBuf.position() > 0) {
					writeBuf.flip();
					peerChannel.bufs.add(writeBuf);
					writeBuf =  ByteBuffer.allocate(BUF_SIZE);
				} else if (writeBuf == null)
					writeBuf =  ByteBuffer.allocate(BUF_SIZE);
			}
			
		}
		
		
		/*============== Implement Output by calling dataOut methods =========*/

		@Override
		public void flush() throws IOException {
			dataOut.flush();
		}

		@Override
		public void write(int b) throws IOException {
			dataOut.writeInt(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			dataOut.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			dataOut.write(b, off, len);
		}
		
		@Override
		public void writeBoolean(boolean arg0) throws IOException {
			dataOut.writeBoolean(arg0);
		}

		@Override
		public void writeByte(int arg0) throws IOException {
			dataOut.writeByte(arg0);
		}

		@Override
		public void writeBytes(String arg0) throws IOException {
			dataOut.writeBytes(arg0);
		}

		@Override
		public void writeChar(int arg0) throws IOException {
			dataOut.writeChar(arg0);
		}

		@Override
		public void writeChars(String arg0) throws IOException {
			dataOut.writeChars(arg0);
		}

		@Override
		public void writeDouble(double arg0) throws IOException {
			dataOut.writeDouble(arg0);
		}

		@Override
		public void writeFloat(float arg0) throws IOException {
			dataOut.writeFloat(arg0);
		}

		@Override
		public void writeInt(int arg0) throws IOException {
			dataOut.writeInt(arg0);
		}

		@Override
		public void writeLong(long arg0) throws IOException {
			dataOut.writeLong(arg0);
		}

		@Override
		public void writeShort(int arg0) throws IOException {
			dataOut.writeShort(arg0);
		}

		@Override
		public void writeUTF(String arg0) throws IOException {
			dataOut.writeUTF(arg0);
		}


        @Override
        public void writeNullableObject(Object obj) throws IOException {
            dataOut.writeNullableObject(obj);
        }

        @Override
		public void writeObject(Object obj) throws IOException {
			dataOut.writeObject(obj);
			
		}

		/*============== Implement input by calling dataIn methods =========*/
		@Override
		public int available() throws IOException {

			return dataIn.available();
		}

		@Override
		public int read() throws IOException {
			return dataIn.read();
		}

		@Override
		public int read(byte[] b) throws IOException {
			return dataIn.read(b);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return dataIn.read(b, off, len);
		}


		@Override
		public long skip(long n) throws IOException {
			return dataIn.skip(n);
		}

		@Override
		public boolean readBoolean() throws IOException {
			return dataIn.readBoolean();
		}

		@Override
		public byte readByte() throws IOException {
			return dataIn.readByte();
		}

		@Override
		public char readChar() throws IOException {
			return dataIn.readChar();
		}

		@Override
		public double readDouble() throws IOException {
			return dataIn.readDouble();
		}

		@Override
		public float readFloat() throws IOException {
			return dataIn.readFloat();
		}

		@Override
		public void readFully(byte[] arg0) throws IOException {
			dataIn.readFully(arg0);
		}


		@Override
		public int readInt() throws IOException {
			return dataIn.readInt();
		}

		@SuppressWarnings("deprecation")
		@Override
		public String readLine() throws IOException {
			return dataIn.readLine();
		}

		@Override
		public long readLong() throws IOException {
			return dataIn.readLong();
		}

		@Override
		public short readShort() throws IOException {
			return dataIn.readShort();
		}

		@Override
		public String readUTF() throws IOException {
			return dataIn.readUTF();
		}

		@Override
		public int readUnsignedByte() throws IOException {
			return dataIn.readUnsignedByte();
		}

		@Override
		public int readUnsignedShort() throws IOException {
			return dataIn.readUnsignedShort();
		}

		@Override
		public int skipBytes(int arg0) throws IOException {
			return dataIn.skipBytes(arg0);
		}


		@Override
		public <T> T readObject(Class<T> type) throws IOException {
			return dataIn.readObject(type);
		}


		@Override
		public <T> T readObject(T obj) throws IOException {
			return dataIn.readObject(obj);
		}

        @Override
        public <T> T readNullableObject(Class<T> type) throws IOException {
            return dataIn.readNullableObject(type);
        }


        @Override
		public void readFully(byte[] b, int off, int len) throws IOException {
			dataIn.readFully(b, off, len);
		}
		
	}
	
	public Channel[] getChannelPair(String name) {
		LocalChannel a = new LocalChannel(name + "-0");
		LocalChannel b = new LocalChannel(name + "-1");
		
		a.connect(b);

        Channel[] pair = { a, b };
		
        return pair;
	}
	
	public static int totalChannels = 0;
    public Channel[] getChannelPair() {
		++totalChannels;
		return getChannelPair("chan"+totalChannels);
	}
}
