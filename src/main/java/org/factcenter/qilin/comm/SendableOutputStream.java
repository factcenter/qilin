package org.factcenter.qilin.comm;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Collection;

public class SendableOutputStream extends DataOutputStream implements SendableOutput {
	public SendableOutputStream(OutputStream out) {
		super(out);
	}

    @Override
    public void writeNullableObject(Object obj) throws IOException {
        writeBoolean(obj == null);
        if (obj != null)
            writeObject(obj);
    }

    @Override
	public void writeObject(Object obj) throws IOException {
        if (obj == null)
            throw new NullPointerException("use writeNullableObject to write values that may be null!");

        if (obj instanceof byte[]) {
			byte[] arr = (byte []) obj;
			writeInt(arr.length);
			write(arr);
		} else if (obj instanceof short[]) {
			short[] arr = (short []) obj;
			writeInt(arr.length);
			for (short v: arr) {
				writeShort(v);
			}
		} else if (obj instanceof char[]) {
			char[] arr = (char []) obj;
			writeInt(arr.length);
			for (char v: arr) {
				writeChar(v);
			}
		} else if (obj instanceof int[]) {
			int[] arr = (int []) obj;
			writeInt(arr.length);
			for (int v: arr) {
				writeInt(v);
			}
		} else if (obj instanceof long[]) {
			long[] arr = (long []) obj;
			writeInt(arr.length);
			for (long v: arr) {
				writeLong(v);
			}
		} else if (obj instanceof float[]) {
			float[] arr = (float []) obj;
			writeInt(arr.length);
			for (float v: arr) {
				writeFloat(v);
			}
			
		} else if (obj instanceof double[]) {
			double[] arr = (double []) obj;
			writeInt(arr.length);
			for (double v: arr) {
				writeDouble(v);
			}
		} else if (obj instanceof Enum<?>) {
            // Enum
            int numVals = obj.getClass().getEnumConstants().length;
            Enum<?> e = (Enum<?>) obj;
            int ord = e.ordinal();
            if (numVals < 0x100) {
                write((byte) ord);
            } else if (numVals < 0x10000) {
                writeShort(ord);
            } else {
                writeInt(ord);
            }
        } else if (obj instanceof Collection) {
			@SuppressWarnings("rawtypes")
			Collection col = (Collection) obj;
			writeInt(col.size());
			for (Object v : col) {
				writeObject(v);
			}
		} else if (obj instanceof Object[]) {
			Object[] arr = (Object []) obj;
			writeInt(arr.length);
			for (Object v: arr) {
				writeObject(v);
			}
		} else if (obj instanceof Sendable) {
			((Sendable) obj).writeTo(this);
		} else if (obj instanceof BigInteger) {
			BigInteger bint = (BigInteger) obj;
			byte[] bytes = bint.toByteArray();
			writeInt(bytes.length);
			write(bytes);
		} else {
			throw new RuntimeException("Writing unsupported object type to SendableOutputStream: " + obj.getClass());
		}
	}
}
