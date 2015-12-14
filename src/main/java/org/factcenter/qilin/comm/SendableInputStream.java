package org.factcenter.qilin.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Collection;



public class SendableInputStream extends DataInputStream implements SendableInput {
	final Logger logger = LoggerFactory.getLogger(getClass());
	
	public SendableInputStream(InputStream in) {
		super(in);
	}


    public <T> T readNullableObject(Class<T> type) throws IOException {
        boolean isNull = readBoolean();
        if (isNull)
            return null;
        else
            return readObject(type);
    }


    @SuppressWarnings("unchecked")
	@Override
	public <T> T readObject(Class<T> type) throws IOException {
		Class<?> componentType = type.getComponentType();
		if (componentType != null) {
			// This is an array
			int len = readInt();
			Object arr = Array.newInstance(componentType, len);
			readArray(arr, len);
			return (T) arr;
		} else if (type == BigInteger.class) {
			int length = readInt();
			byte[] bytes = new byte[length];
			readFully(bytes);
			return (T) new BigInteger(bytes);
		} else if (type.isEnum()) {
            T[] vals = type.getEnumConstants();
            int numVals = vals.length;
            int ord;
            if (numVals < 0x100) {
                ord = read();
            } else if (numVals < 0x10000) {
                ord = readShort();
            } else {
                ord = readInt();
            }
            if (ord == -1)
                throw new EOFException("Remote closed stream");
            return vals[ord];
        } else {
			Sendable obj;
			try {
				obj = (Sendable) type.newInstance();
			} catch (InstantiationException e) {
				String err = "Can't create new instance of type " + type + ": " + e.getMessage();
				logger.error(err);
				throw new RuntimeException(err);
			} catch (IllegalAccessException e) {
				String err = "Illegal access creating new instance of type " + type + ": " + e.getMessage();
				logger.error(err);
				throw new RuntimeException(err);
			}
			return readObject((T) obj);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T readObject(T obj) throws IOException {
		if (obj instanceof Sendable) {
			((Sendable) obj).readFrom(this);
		} else if (obj.getClass().isArray()) {
			int len = readInt();
			if (Array.getLength(obj) != len) {
				obj = (T) Array.newInstance(obj.getClass().getComponentType(), len);
			}
			readArray(obj, len);
		} else if (obj instanceof Collection) {
			int len = readInt();
			readArray(obj, len);
		} else if (obj instanceof BigInteger) {
			// BigInteger is immutable, we'll have to create a new one.
			return (T) readObject(BigInteger.class);
		} else {
			String err = "Reading object of unsupported type: " + obj.getClass();
			logger.error(err);
			throw new RuntimeException(err);
		}
		return obj;
	}


	/**
	 * Read from the stream into an array or collection. The array is assumed to be of the correct length. 
	 * If it is an array of objects, existing objects will be overwritten using {@link #readObject(Object)}, otherwise
	 * a new object will be created using {@link #readObject(Class)}. For collections, at least the first object must
	 * exist (otherwise we cannot tell what type of objects to read). If the collection is longer than len, it will be emptied,
	 * and new objects will be added.
	 * 
	 * @param obj
	 * @param len
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	void readArray(Object obj, int len) throws IOException {
		if (obj instanceof byte[]) {
			byte[] arr = (byte []) obj;
			readFully(arr);
		} else if (obj instanceof short[]) {
			short[] arr = (short []) obj;
			for (int i = 0; i < len; ++i) {
				arr[i] = readShort();
			}
		} else if (obj instanceof char[]) {
			char[] arr = (char []) obj;
			for (int i = 0; i < len; ++i) {
				arr[i] = readChar();
			}
		} else if (obj instanceof int[]) {
			int[] arr = (int []) obj;
			for (int i = 0; i < len; ++i) {
				arr[i] = readInt();
			}
		} else if (obj instanceof long[]) {
			long[] arr = (long []) obj;
			for (int i = 0; i < len; ++i) {
				arr[i] = readLong();
			}
		} else if (obj instanceof float[]) {
			float[] arr = (float []) obj;
			for (int i = 0; i < len; ++i) {
				arr[i] = readFloat();
			}
		} else if (obj instanceof double[]) {
			double[] arr = (double []) obj;
			for (int i = 0; i < len; ++i) {
				arr[i] = readDouble();
			}
		} else if (obj instanceof Collection) {
			// Collections are implemented with type erasure, so we can't tell
			// which class we need to construct just from the collection object.
			// Therefore, we only support reading into collections if at least the first object
			// is initialized.
			@SuppressWarnings("rawtypes")
			Collection col = (Collection) obj;
			if (col.isEmpty())
				throw new RuntimeException("Can't read into a collection of unknown type!");
			
			Class<?> colType = col.iterator().next().getClass();
			
			if (col.size() > len)
				col.clear();
			
			int i = 0;
			for (Object v : col) {
				readObject(v);
				if (++i >= len)
					break;
			}
			
			for (; i < len; ++i) {
				Object newObj = readObject(colType);
				col.add(newObj);
			}
		} else if (obj instanceof Object[]) {
			Object[] arr = (Object []) obj;
			for (int i = 0; i < len; ++i) {
				if (arr[i] == null)
					arr[i] = readObject(arr.getClass().getComponentType());
				else 
					arr[i] = readObject(arr[i]);
			}
		} else 
			throw new RuntimeException("Unknown array type: " + obj.getClass());
	}
	
}
