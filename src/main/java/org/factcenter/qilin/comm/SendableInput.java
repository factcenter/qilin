package org.factcenter.qilin.comm;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.math.BigInteger;
import java.util.Collection;

/**
 * Extend {@link DataInput} with ability to read certain types of objects:
 * primitive arrays, {@link Collection}s, {@link BigInteger}s and objects implementing {@link Sendable}.
 * 
 * Unlike {@link ObjectInput}, the type of object to be read <i>must be known in advance</i>. This allows
 * zero overhead in the stream (object identifiers aren't sent) and a type-safe API.
 *  
 * Reading an unsupported object type will cause a runtime exception.
 * @author talm
 *
 */
public interface SendableInput extends DataInput {

	/**
	 * @see InputStream#available()
	 * @throws IOException
	 */
	public int available() throws IOException;

	/**
	 * @see InputStream#read()
	 * @throws IOException
	 */
	public int read() throws IOException;	

	/**
	 * @see InputStream#read(byte[])
	 * @throws IOException
	 */
	public int read(byte[] b) throws IOException;

	/**
	 * @see InputStream#read(byte[], int, int)
	 * @throws IOException
	 */
	public int read(byte[] b, int off, int len) throws IOException;

	/**
	 * @see InputStream#skip(long)
	 * @throws IOException
	 */
	public long skip(long n) throws IOException;

	/**
	 * Read an object of known type.
	 * The object type must have a default constructor as it will be 
	 * constructed with a call to {@link Class#newInstance()}.
     * The read object will not be null.
	 * @param type
	 * @return
	 */
	public <T> T readObject(Class<T> type) throws IOException;
	
	/**
	 * Read into an existing object. 
	 * If the existing object is an array but not of the proper length, a new array is returned instead.
	 * The object must be either a primitive array, a collection (with at least one element of a supported type) 
	 * or an instance of {@link Sendable}. Objects of dif 
	 * @param obj
	 * @return obj, or a new object containing the read data if obj is unsuitable.
	 */
	public <T> T readObject(T obj) throws IOException;


    /**
     * Read an object of a known type, possibly null.
     * This should have bee
     * @param type
     * @param <T>
     * @return
     * @throws IOException
     */
    public  <T> T readNullableObject(Class<T> type) throws IOException;
}
