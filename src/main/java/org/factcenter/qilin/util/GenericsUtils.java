package org.factcenter.qilin.util;

import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;

import java.io.IOException;
import java.util.Arrays;



/**
 * A utility class that helps overcome some quirks that arise in Java generics programming.
 * @author talm
 *
 */
public class GenericsUtils {
	/**
	 * Test for equality, if types are arrays compare array contents (deep equals), otherwise compare
	 * objects. 
	 * @param <T>
	 * @param a
	 * @param b
	 */
	public static <T> boolean deepEquals(T a, T b) {
		if (a == null)
			return b == null;
		
		if (b == null)
			return a == null;
		
		Class<?> classT = a.getClass();
		
		if (!classT.isArray()) {
			return (a.equals(b));
		} else if (classT.equals(boolean[].class)) {
			return Arrays.equals((boolean[]) a, (boolean[]) b);
		} else if (classT.equals(byte[].class)) {
			return Arrays.equals((byte[]) a, (byte[]) b);
		} else if (classT.equals(short[].class)) {
			return Arrays.equals((short[]) a, (short[]) b);
		} else if (classT.equals(int[].class)) {
			return Arrays.equals((int[]) a, (int[]) b);
		} else if (classT.equals(long[].class)) {
			return Arrays.equals((long[]) a, (long[]) b);
		} else if (classT.equals(char[].class)) {
			return Arrays.equals((char[]) a, (char[]) b);
		} else if (classT.equals(float[].class)) {
			return Arrays.equals((float[]) a, (float[]) b);
		} else if (classT.equals(double[].class)) {
			return Arrays.equals((double[]) a, (double[]) b);
		}  else {
			return Arrays.deepEquals((Object[]) a, (Object[]) b);
		}
		
	}
	
	/**
	 * Map class that allows primitive arrays to behave as expected
	 * 
	 */
	public static class HashMap<K,V> extends java.util.HashMap<K, V> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public HashMap() {
			super();
		}

		public HashMap(final int initialCapacity) {
			super(initialCapacity);
		}

		@Override
		public boolean containsKey(final Object key) {
			return super.containsKey(wrap(key));
		}

		@Override
		public V get(final Object key) {
			return super.get(wrap(key));
		}

		@SuppressWarnings(value="unchecked")
		@Override
		public V put(final K key, final V value) {
			return super.put((K) wrap(key), value);
		}

		@Override
		public V remove(final Object key) {
			return super.remove(wrap(key));
		}
	}
	
	// @SuppressWarnings(value="unchecked")
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object wrap(Object item) {
		if (item == null)
			return null;

		Class<?> classT = item.getClass();
		
		if (classT.isArray()) {
			return new Wrapper(item);
		} else
			return item;
	}
	
	
	/**
	 * Wrapper class that allows hashing and equals for primitive arrays
	 * to behave as expected.
	 * @author talm
	 *
	 * @param <T>
	 */
	public static class Wrapper<T> {
		public T item;
		public Wrapper(T item) {
			this.item = item;
		}
		
		@SuppressWarnings(value="unchecked")
		@Override
		public boolean equals(Object b) {
			if (b instanceof Wrapper) {
				return deepEquals(item, ((Wrapper<T>) b).item);
			} else
				return deepEquals(item, b);
		}
		
		@Override
		public int hashCode() {
			Class<?> classT = item.getClass();
			
			if (!classT.isArray()) {
				return (item.hashCode());
			} else if (classT.equals(boolean[].class)) {
				return Arrays.hashCode((boolean[]) item);
			} else if (classT.equals(byte[].class)) {
				return Arrays.hashCode((byte[]) item);
			} else if (classT.equals(short[].class)) {
				return Arrays.hashCode((short[]) item);
			} else if (classT.equals(int[].class)) {
				return Arrays.hashCode((int[]) item);
			} else if (classT.equals(long[].class)) {
				return Arrays.hashCode((long[]) item);
			} else if (classT.equals(char[].class)) {
				return Arrays.hashCode((char[]) item);
			} else if (classT.equals(float[].class)) {
				return Arrays.hashCode((float[]) item);
			} else if (classT.equals(double[].class)) {
				return Arrays.hashCode((double[]) item);
			}  else {
				return Arrays.deepHashCode((Object[]) item);
			}
		}
		
		public static <T> ByteEncoder<Wrapper<T>> getEncoder(final ByteEncoder<T> innerEncoder) {
			return new ByteEncoder<Wrapper<T>>() {
				@Override
				public Wrapper<T> decode(byte[] input) {
					return new Wrapper<T>(innerEncoder.decode(input));
				}

				@Override
				public Wrapper<T> denseDecode(byte[] input) {
					return new Wrapper<T>(innerEncoder.denseDecode(input));
				}

				@Override
				public byte[] encode(Wrapper<T> input) {
					return innerEncoder.encode(input.item);
				}

				@Override
				public int getMinLength() {
					return innerEncoder.getMinLength();
				}
			};
		}
		
		public static <T> StreamEncoder<Wrapper<T>> getStreamEncoder(final StreamEncoder<T> innerEncoder) {
			return new StreamEncoder<Wrapper<T>>() {
//				@Override
//				public Wrapper<T> decode(InputStream in) throws IOException {
//					return new Wrapper<T>(innerEncoder.decode(in));
//				}
//
//				@Override
//				public int encode(Wrapper<T> input, OutputStream out)
//						throws IOException {
//					return innerEncoder.encode(input.item, out);
//				}

				@Override
				public void encode(Wrapper<T> input, SendableOutput out)
						throws IOException {
					innerEncoder.encode(input.item, out);
				}

				@Override
				public Wrapper<T> decode(SendableInput in) throws IOException {
					return new Wrapper<T>(innerEncoder.decode(in));
				}
			};
		}

	}
}
