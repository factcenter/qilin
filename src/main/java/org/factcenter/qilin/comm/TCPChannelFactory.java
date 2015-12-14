package org.factcenter.qilin.comm;

import org.factcenter.qilin.util.NonInterruptable;

import java.io.IOException;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Class for handling TCP connections. 
 * The TCPChannelFactory
 * is responsible for creating TCP connections to a peer (given a host/port pair),
 * and for reading and writing messages from/to the peer. 
 * TCPChannel implements a multiplexing server -- after creating the factory
 * with a local port value, the run() method should be called (the recommended
 * way is by creating a new thread). The TCPChannel then listens on the port
 * for incoming connections and handles message transmission and reception. 
 * @author talm
 *
 */
public class TCPChannelFactory implements Runnable {
	boolean shouldRun;
	
	/**
	 * The local port number on which the server listens
	 */
	int localport;
	
	/**
	 * The name of the local host
	 */
	String localname;
	
	/**
	 * The local server channel.
	 */
	ServerSocketChannel serverChannel;
	
	/**
	 * Selector for server channel.
	 */
	Selector selector;
	
	/**
	 * Currently pending accept requests. Each element in the queue contains
	 * a queue. When a connection is accepted, the new channel will
	 * be placed on the next available request queue.
	 */
	BlockingQueue<TCPChannel> acceptedChannels;
	
	/**
	 * Resolve a peer name into a socket address. Accepts names of the 
	 * form host:port.
	 * @param name
	 * @return {@link InetSocketAddress} corresponding to the name, or null if name is not of the form host:port.
	 */
	InetSocketAddress resolve(String name) {
		String[] hostport = name.split(":", 2);
		if (hostport.length != 2)
			return null;
		return new InetSocketAddress(hostport[0], Integer.valueOf(hostport[1]));
	}
	
	
	public int getLocalPort() {
		return localport;
	}
	
	/**
	 * Get a communication channel to a specified peer. 
	 * @param peerName a label for the peer. 
	 * @return A channel through which messages can be sent to and 
	 * 		received from the peer.
	 */
	public TCPChannel getChannel(String peerName) throws IOException {
		TCPChannel newPeer = new TCPChannel(peerName);
		if (newPeer.addr == null) {
			// Name resolution failed for peerName
			throw new UnknownHostException(peerName);
		}
			
		//try {
			newPeer.channel = SocketChannel.open();
			newPeer.channel.configureBlocking(true);
			newPeer.channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
			newPeer.state = ChannelConnectionState.CONNECTING;
			boolean ok = newPeer.channel.connect(newPeer.addr);
			if (!ok)
				ok = newPeer.channel.finishConnect();
			
			if (ok) {
				newPeer.initSocketStreams();
				writeConnectHandshake(newPeer);
				newPeer.state = ChannelConnectionState.CONNECTED;
			} else {
				newPeer.state = ChannelConnectionState.CLOSED;
			}
		/* } catch (IOException ioe) {
			// Connection error -- we return a "bad" channel.
			newPeer.state = ChannelConnectionState.CLOSED;
		} */

		return newPeer;
	}
	
	/**
	 * Get a communication channel by waiting for the next peer to connect 
	 * @return A channel through which messages can be sent to and 
	 * 		received from the peer.
	 */
	public TCPChannel getChannel() throws IOException {
		TCPChannel peer = NonInterruptable.take(acceptedChannels);
		
		if (peer.state == ChannelConnectionState.CLOSED)
			// This could happen if there was a connection error.
			return null;

		peer.initSocketStreams();
		handleAcceptHandshake(peer);
		return peer;
	}
	
	
	
	enum ChannelConnectionState {
		CLOSED,					// not connected
		CONNECTING,				// Waiting for TCP Connection to a peer
		ACCEPT_HANDSHAKE_RECV,	// Receive handshake bytes (acceptor)
		CONNECTED				// Connected to peer
	};

	final static int HANDSHAKE_ACK = 0;
	final static int HANDSHAKE_NACK = 1;
	
	public class TCPChannel implements Channel {
		/**
		 * Name of peer.
		 */
		String name;
		
		/**
		 * Address of peer
		 */
		InetSocketAddress addr;
		
		/**
		 * Channel connected to peer.
		 */
		SocketChannel channel;
		
		/**
		 * The stream from which data can be read.
		 */
		SendableInputStream dataIn;

		/**
		 * The stream from which data can be read.
		 */
		SendableOutputStream dataOut;
		
		
		/**
		 * Is this channel connected to a peer.
		 */
		ChannelConnectionState state;

	
		/**
		 * Construct an unconnected TCPChannel to a known peer.
		 * If peername does not resolve correctly, addr will be null. 
		 * @param peername
		 */
		TCPChannel(String peername) {
			name = peername;
			addr = resolve(peername);
			state = ChannelConnectionState.CLOSED;
		}
		

		/**
		 * Construct a TCPChannel from a connected channel;
		 * we won't know the peer's name until the handshake is finished.
		 */
		TCPChannel(SocketChannel channel) {
			this.channel = channel;
			name = null;
			addr = (InetSocketAddress) channel.socket().getRemoteSocketAddress();
			state = ChannelConnectionState.ACCEPT_HANDSHAKE_RECV;
		}
		
		void initSocketStreams() throws IOException {
			dataOut = new SendableOutputStream(channel.socket().getOutputStream());
			dataIn = new SendableInputStream(channel.socket().getInputStream());
		}

		public String getName() {
			return name;
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

	/**
	 * Construct a TCP channel factory that acts as a server.
	 * @param localport the port to wait on (0 means the first open port).
	 * @throws IOException
	 */
	public TCPChannelFactory(int localport) throws IOException {
		shouldRun = true;
		
		this.localport = localport;
		InetAddress addr = InetAddress.getLocalHost();
		
		serverChannel = ServerSocketChannel.open();
		ServerSocket socket = serverChannel.socket();
		if (localport == 0) {
			socket.bind(null);
			this.localport = socket.getLocalPort();
		} else { 
			socket.bind(new InetSocketAddress(localport));
		}

		this.localname = addr.getHostName() + ":" + this.localport;
		serverChannel.configureBlocking(false);
		
		selector = Selector.open();
		
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		acceptedChannels = new LinkedBlockingQueue<TCPChannel>();
	}
	
	
	/**
	 * Act as a server on the first open port.
	 * @throws IOException
	 */
	public TCPChannelFactory() throws IOException {
		this(0);
	}
	
	
	public void stop() {
		shouldRun = false;
		selector.wakeup();
	}
	/**
	 * Start a loop that is responsible for sending and receiving messages
	 * and delivering them to local destinations.
	 */
	@Override
	public void run() {
		while(shouldRun) {
			try {
				selector.select();
				
				for (SelectionKey key : selector.selectedKeys()) {
					if (key.isAcceptable()) {
						// Received a new connection
						acceptNewConnection(key);
					} 
				}
			} catch (IOException ioe) {
				// TODO: Deal with exceptions more robustly
				shouldRun = false; // Just exit
			}
		}
	}
	
	
	void acceptNewConnection(SelectionKey key) {
		ServerSocketChannel server = (ServerSocketChannel) key.channel();
		
		try {
			SocketChannel channel = server.accept();
			if (channel != null) {
				channel.configureBlocking(true);
				channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
				TCPChannel peer = new TCPChannel(channel);
				acceptedChannels.put(peer);
			}
		} catch (IOException ioe) {
			// Ignore, the connection just died.
		} catch (InterruptedException e) {
			// Ignore
		} 
	}

	/**
	 * Generate a handshake message for the peer.
	 * Handshake consists of localname.
	 * @param peer
	 * @return the handshake message
	 */
	void writeConnectHandshake(TCPChannel peer) throws IOException {
		peer.writeUTF(localname);
		peer.flush();
	}

	
	/**
	 * Handle the accept handshake from the peer.
	 * @param peer
	 */
	void handleAcceptHandshake(TCPChannel peer) throws IOException {
		// The handshake should contain the peer name.
		peer.name = (String) peer.readUTF();
	}
	

	public String getLocalname() {
		return localname;
	}
}

