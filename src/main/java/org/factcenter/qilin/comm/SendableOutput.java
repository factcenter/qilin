package org.factcenter.qilin.comm;

import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;

/**
 * Extend {@link DataOutput} with ability to write certain types of objects:
 * primitive arrays, {@link Collection}s, {@link BigInteger}s and {@link Sendable}s.
 * Writing other objects will cause a runtime exception.
 * 
 * @author talm
 *
 */
public interface SendableOutput extends DataOutput {
    /**
     * Write an object that can be null.
     * Objects written with this method must be read with {@link SendableInput#readNullableObject(Class)}.
     * @param obj
     * @throws IOException
     */
    public void writeNullableObject(Object obj) throws IOException;

    /**
     * Write an object. The passed object may not be null.
     * @param obj
     * @throws IOException
     */
	public void writeObject(Object obj) throws IOException;
	
	public void flush() throws IOException;
}
