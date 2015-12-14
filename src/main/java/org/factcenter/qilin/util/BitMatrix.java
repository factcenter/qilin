package org.factcenter.qilin.util;

import org.factcenter.qilin.comm.Sendable;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 * Represents a matrix of bits with corresponding operations.
 * @author talm
 *
 */
public final class BitMatrix implements Sendable, Cloneable {
	
	/**
	 * The actual bits (packed). Bits are stored in row order, and every row is padded to byte boundaries
	 * (i.e., if there are 15 bits per row, each row is 2 bytes long). Within each byte, the LSB is bit 0 
	 * and the MSB bit 7.
	 */
	byte[] bits;
	
	/**
	 * Number of columns.
	 */
	int numCols;
	
	/**
	 * Number of rows.
	 */
	int numRows;
	
	/**
	 * true iff the number of columns is divisible by 8 or any remaining bits in the final byte of each row are zeroes.
	 */
	boolean zeroPadded;

    /**
     * If the matrix is sharing its backing array with another, and should copy the relevant bits
     * during write.
     */
    boolean copyOnWrite;
	
	/**
	 * Actual byte offset in {@link #bits} where array starts.
	 */
	transient int rowOffs;

	
	/**
	 *  Number of bytes in a row.
	 *  This is usually {@link #numCols} / 8 rounded up, but can be more 
	 *  (in which case the extra bytes in the row are padding and should be ignored).
	 */
	transient int bytesPerRow;


    /**
     * How many bytes are used for a row with a specified number of columns.
     * @param numCols
     */
    public final int getUsedBytesPerRow(int numCols) {
        return (numCols / 8) + (numCols % 8 != 0 ? 1 : 0);
    }

	/**
	 * Return the number of bytes actually used in row.
	 */
	public final int getUsedBytesPerRow() {
		return getUsedBytesPerRow(numCols);
	}
	
	/**
	 * Initialize bits array and precomputed values.
	 */
	private void allocate() {
		bytesPerRow = getUsedBytesPerRow();

		bits = new byte[bytesPerRow * numRows];
		rowOffs = 0;
		zeroPadded = true;
        copyOnWrite = false;
	}
	
	/**
	 * Don't ever call this explicitly: used only for deserialization!
	 */
	public BitMatrix() {
		// Used only for deserialization
	}
	
	/**
	 * Create a new BitMatrix with numRows rows and numCols columns. To create a bit <i>vector</i>, set numRows to 1.
	 * @param numCols
	 * @param numRows
	 */
	public BitMatrix(int numCols, int numRows) {
		this.numCols = numCols;
		this.numRows = numRows;
		allocate();
	}

	/**
	 * Utility constructor for bit vector.
	 */
	public BitMatrix(int numCols) {
		this(numCols, 1);
	}
	
	/**
	 * Create a vector by wrapping a byte array.
	 * Note that the backing array is <i>not</i> copied---changes
	 * to this array will be reflected in the vector, but copy-on-write
     * semantics are used for BitMatrix operations..
	 */
	public BitMatrix(byte[] bits, int offs) {
		this.bits = bits;
		this.rowOffs = offs;
		bytesPerRow = bits.length - offs;
		numCols = bytesPerRow * 8;
		numRows = 1;
        copyOnWrite = true;
		zeroPad();
	}
	
	/**
	 * Create a matrix by wrapping a byte array.
	 * Note that the backing array is <i>not</i> copied---changes
	 * to this array will be reflected in the matrix.
	 * @param bits
	 * @param numCols
	 * @param numRows
	 */
	public BitMatrix(byte[] bits, int rowOffs, int numCols, int numRows) {
		this.bits = bits;
		this.rowOffs = rowOffs;
		this.numCols = numCols;
		this.numRows = numRows;
		if ((bits.length - rowOffs) * 8 < numCols * numRows)
			throw new RuntimeException("Invalid BitMatrix size (numCols="+numCols+",bytesPerRow="+bytesPerRow+")");
		bytesPerRow = getUsedBytesPerRow();
        copyOnWrite = true;
		zeroPad();
	}
	
	/**
	 * Create a matrix by wrapping a byte array.
	 * Note that the backing array is <i>not</i> copied---changes
	 * to this array will be reflected in the matrix unless the
	 * provided array is too small (in that case a new array will be allocated).
	 * @param bits
	 * @param numCols
	 * @param numRows
	 */
	public BitMatrix(byte[] bits, int rowOffs, int numCols, int numRows, int bytesPerRow, boolean copyOnWrite,
                     boolean zeroPadding) {
		this.bits = bits;
		this.rowOffs = rowOffs;
		this.numCols = numCols;
		this.numRows = numRows;
        this.copyOnWrite = copyOnWrite;
		this.bytesPerRow = bytesPerRow;
		
		if (numCols > bytesPerRow * 8)
			throw new RuntimeException("Invalid BitMatrix size (numCols="+numCols+",bytesPerRow="+bytesPerRow+")");
		
		if ((bits.length - rowOffs) * 8 < numCols * numRows)
			throw new RuntimeException("Invalid BitMatrix size (numCols="+numCols+",bytesPerRow="+bytesPerRow+")");
		
		if (zeroPadding)
			zeroPad();
		else {
			if (numCols % 8 != 0)
				zeroPadded = false;
		}
	}

    /**
     * Create a new bitmatrix sharing a backing array with an existing one, and using copy-on-write semantics.
     * @param bits
     * @param rowOffs
     * @param numCols
     * @param numRows
     * @param bytesPerRow
     */
	public BitMatrix(byte[] bits, int rowOffs, int numCols, int numRows, int bytesPerRow) {
		this(bits, rowOffs, numCols, numRows, bytesPerRow, true, true);
	}

    /**
     * Return a new BitMatrix representing an (unsigned) integer value.
     * The matrix contains a single row of width columnts, with bits stored in LSB-first order.
     * @param value The value to represent
     * @param width the width in bits.
     */
    public static BitMatrix valueOf(long value, int width) {
        BitMatrix b = new BitMatrix(width);
        b.setBits(0, width, value);

        return b;
    }


    public static BitMatrix valueOf(BigInteger value, int width) {
        BitMatrix b = new BitMatrix(width);

        byte[] valBits = value.toByteArray();
        long sign = value.signum() < 0 ? -1 : 0;

        // Note: we use the fact the setBits can handle writing to positions outside the
        // BitMatrix by truncating.
        for (int i = 0; i < width / 8 + 1; ++i) {
            long valOctet;

            if (i >= valBits.length) {
                valOctet = sign;
            } else {
                valOctet = valBits[valBits.length - i - 1];
            }
            b.setBits(i * 8, 8, valOctet);
        }

        return b;
    }

    /**
     * Return a bit matrix with all bits set.
     * @param width
     */
    public static BitMatrix allOnes(int width) {
        BitMatrix b = new BitMatrix(width);

        for (int i = 0; i < b.bits.length; ++i) {
            b.bits[i] = (byte) 0xff;
        }
        b.zeroPad();
        return b;
    }

    final public long toInteger() {
        return toInteger(getNumCols());
    }

    /**
     * Convert the least-significant width bits of the first row in this matrix
     * to an integer (well-defined only for width up to 64).
     * @param width
     */
    final public long toInteger(int width) {
        if (numCols < width)
            width = numCols;

        int startIdx = getRowIndex(0);
        long value = 0;

        if (width % 8 != 0) {
            long mask = ((1 << (width % 8)) - 1);
            value = ((int) bits[startIdx + width / 8]) & mask;
        }
        for (int i = width / 8 - 1; i >= 0; --i) {
            value <<= 8;
            value |= ((int) bits[startIdx + i]) & 0xff;
        }

        return value;
    }


    /**
     * Convert the entire vector (first row) into a BigInteger.  The value is always treated as unsigned in little-endian
     * order (LSB first).
     */
    final public BigInteger toBigInteger() {
        return toBigInteger(getNumCols());
    }

    /**
     * Convert the least-significant width bits of the first row in
     * this matrix to a BigInteger. The value is always treated as unsigned in little-endian
     * order (LSB first).
     *
     * @param width
     */
    final public BigInteger toBigInteger(int width) {
        if (width > getNumCols())
            width = getNumCols();

        int startIdx = getRowIndex(0);
        int byteLen = width / 8;

        if (width % 8 != 0)
            ++byteLen;

        byte[] bigBits = new byte[byteLen];

        for (int i = 0; i < byteLen; ++i) {
            bigBits[byteLen - i - 1] = bits[startIdx + i];
        }

        if (width % 8 != 0) {
            int mask = (1 << (width % 8)) - 1;
            bigBits[0] &= mask;
        }

        return new BigInteger(1, bigBits);
    }


    /**
     * Clone the current BitMatrix into a byte array in packed form, starting from a given column.
     * The destination array must have enough space to contain the data.
     * @param dst
     * @param colStart
     * @param numCols
     */
    final public void clonePacked(byte[] dst, int colStart, int numCols) {
        if (colStart + numCols >= this.numCols)
            numCols = this.numCols - colStart;

        int packedRowLen = getUsedBytesPerRow(numCols);
        if (colStart % 8 == 0) {
            int lastColByte = colStart / 8 + numCols / 8;
            int partialByteSize = numCols % 8;

            for (int i = 0; i < getNumRows(); ++i) {
                System.arraycopy(bits, getRowIndex(i) + colStart / 8, dst, i * packedRowLen, packedRowLen);

                if (!zeroPadded && partialByteSize != 0) {
                    dst[i*packedRowLen+lastColByte] &= (1 << partialByteSize) - 1;
                }
            }
        } else {
            // Every byte needs to be shifted.
            int colShift = colStart / 8;
            int bitShift = colStart % 8;
            int bitMask = (1 << bitShift) - 1;

            for (int i = 0; i < getNumRows(); ++i) {
                int rowStart = getRowIndex(i);
                int dstRowStart = i * packedRowLen;
                assert(dst != bits || dstRowStart <= rowStart);
                for (int j = 0; j < packedRowLen; ++j) {
                    dst[dstRowStart + j] = (byte) ((bits[rowStart + colShift + j] & 0xff) >>> bitShift);
                    if (rowStart + colShift + j + 1 < bits.length)
                        dst[dstRowStart + j] |= (bits[rowStart + colShift + j + 1] & bitMask) << (8 - bitShift);
                }
            }
        }
    }

	/**
	 * Reduce the number of columns in a row without allocating a new backing array.
	 * 
	 * <b>Note:</b>The backing array after this operation may not be zero-padded, Moreover, operations that do zero-pad
	 * {@link #xorRow(int, BitMatrix, int)} and {@link #fillRandom(Random)}) 
	 * may change values outside the columns spanned by the submatrix.
	 * 
	 * @param newNumCols
	 */
	public void subcolumns(int colStart, int newNumCols) {
		if (colStart + newNumCols > numCols)
			throw new RuntimeException("Can't increase numCols (numCols="+numCols+",newNumCols="+colStart + newNumCols+")");
		

		if (colStart % 8 != 0) {
            if (copyOnWrite) {
                internalCopy();
            } else {
                // Shift bits in place
                clonePacked(bits, colStart, newNumCols);
            }
            numCols = newNumCols;
            rowOffs = 0;
            bytesPerRow = getUsedBytesPerRow();
            zeroPad();
            return;
        }

        // We need to move the row offset to compensate for entire byte shifts
        numCols = newNumCols;
        rowOffs += colStart / 8;

		if ((newNumCols + colStart) % 8 != 0)
			zeroPadded = false;
	}
	
	/**
	 * Reduce the number of rows without allocating a new backing array.
	 * @param rowStart
	 * @param newNumRows
	 */
	public void subrows(int rowStart, int newNumRows) {
		rowOffs = getRowIndex(rowStart);
		numRows = Math.min(numRows - rowStart, newNumRows);
	}

    /**
     * Copy the data from the current backing array to a new, packed array.
     * @param colStart starting column to copy
     * @param newNumCols number of columns to copy.
     */
    private void internalCopy(int colStart, int newNumCols) {
        if (colStart + newNumCols >= numCols)
            newNumCols = numCols - colStart;

        byte[] newBits = new byte[getNumRows() * getUsedBytesPerRow(newNumCols)];
        clonePacked(newBits, colStart, newNumCols);
        bits = newBits;
        copyOnWrite = false;
        rowOffs = 0;
        numCols = newNumCols;
        bytesPerRow = getUsedBytesPerRow();
        zeroPadded = true;
    }

    /**
     * Ensure that the backing array is not shared with another BitMatrix
     */
    final public void internalCopy() {
        if (copyOnWrite)
            internalCopy(0, getNumCols());
    }

	/**
	 * Create a clone of this BitMatrix (backing array is copied).
	 * The new clone will be in packed format.
	 */
	final public BitMatrix clone() {
		byte[] newbits = getPackedBits(true);
        // No need to copy-on-write, we just created a brand-new bit array.
		return new BitMatrix(newbits, 0, numCols, numRows, getUsedBytesPerRow(), false, true);
	}
	
	
	/**
	 * Return a sub-matrix with the same number of columns but a subset of the rows.
	 * The backing array is <i>shared</i> between the matrices (direct changes in one will appear in the other),
     * but uses copy-on-write semantics (BitMatrix operations that modify the backing array will trigger
     * a full copy).
	 * @param startRow Starting row (inclusive) 
	 * @param numRows number of rows.
	 */
	public BitMatrix getSubMatrix(int startRow, int numRows) {
		if (this.numRows - startRow < numRows)
			throw new RuntimeException("Invalid submatrix size");

        copyOnWrite = true; // we're sharing bits with our new submatrix.
		return new BitMatrix(bits, getRowIndex(startRow), numCols, numRows, bytesPerRow);
	}
	
	/**
	 * Return a sub-matrix with the same number of rows but a subset of the columns.
	 * The backing array may be shared between the matrices (changes in one will appear in the other),
	 * or may be copied (if the start column isn't on a byte boundary).
	 * 
	 * @param startCol Starting column (inclusive) 
	 * @param newNumCols number of columns in new matrix
	 */
	public BitMatrix getSubMatrixCols(int startCol, int newNumCols) {
		BitMatrix subMatrix;
		if (startCol % 8 == 0) {
            copyOnWrite = true; // we're sharing bits with our new submatrix.
			subMatrix = new BitMatrix(bits, rowOffs, numCols, numRows, bytesPerRow, true, false);
            subMatrix.subcolumns(startCol, newNumCols);
		} else { 
            byte[] newBits = new byte[getNumRows() * getUsedBytesPerRow(newNumCols)];
            clonePacked(newBits, startCol, newNumCols);

			subMatrix = new BitMatrix(newBits, 0, newNumCols, numRows, getUsedBytesPerRow(newNumCols), false, true);
		}

		return subMatrix;
	}

	public boolean isZeroPadded() {
		return zeroPadded;
	}
	
	/**
	 * Zero all the padding bits (if any).
	 */
	public void zeroPad() {
        internalCopy();
		if (numCols % 8 != 0) {
			int lastColByte = numCols / 8;
			int partialByteSize = numCols % 8;
			// Zero padding bytes in all rows.
			for (int i = 0; i < getNumRows(); ++i) {
				int row = getRowIndex(i);
			    // Zero partial final byte
				bits[row + lastColByte] &= (1 << partialByteSize) - 1; 					
			}
		}
		zeroPadded = true;
	}

	/**
	 * Fill the matrix with random bits.
	 * Note: currently fills the entire backing array -- so will affect "parents" of submatrices.
	 * @param rand
	 */
	public void fillRandom(Random rand) {
        if (copyOnWrite || getRowIndex(0) != 0 || (bits.length > getNumRows() * bytesPerRow) ||
                bytesPerRow > getUsedBytesPerRow()) {
            internalCopy(0, getNumCols());
        }
        rand.nextBytes(bits);
		zeroPad();
	}
	
	/**
	 * Return the backing array.
	 */
	public final byte[] getBackingArray() {
		return bits;
	}
	
	/**
	 * Return the array in packed format.
	 * The size of the array is guaranteed to be {@link #getNumRows()}*ceil({@link #getNumCols()}/8):
	 * (Each row is byte-aligned). If the backing array is already in packed format and zero-padded, 
	 * this method will not copy the array (otherwise a new array will be allocated and the bits copied)  
	 */
	public final byte[] getPackedBits(boolean forceCopy) {
		int packedRowLen = getUsedBytesPerRow();
		if (!forceCopy && zeroPadded && rowOffs == 0 && packedRowLen == getRowLen() && getRowIndex(getNumRows()) == bits.length)
			return bits;

        byte[] newArr = new byte[getNumRows() * packedRowLen];
        clonePacked(newArr, 0, getNumCols());
        return newArr;
	}
	
	public final int getNumRows() {
		return numRows;
	}
	
	public final int getNumCols() {
		return numCols;
	}
	
	/**
	 * Return the byte index in the backing array for the beginning of a row.
	 * @param row
	 */
	public final int getRowIndex(int row) {
		return rowOffs + row * bytesPerRow;
	}
	
	/**
	 * Return number of bytes in a row. 
	 * Note that if the number of columns is not divisible by 8,
	 * the last byte of the row will be padded with zero bits.
	 */
	public final int getRowLen() {
		return bytesPerRow;
	}
	
	
	/**
	 * Return a row as a {@link ByteBuffer} (by wrapping the backing array). 
	 * @param row the row index.
	 */
	public final ByteBuffer getRow(int row) {
		return ByteBuffer.wrap(bits, getRowIndex(row), bytesPerRow);
	}
	
	/**
	 * Return the bit at position (col, row) in the matrix.
	 * @param row the row index (zero based)
	 * @param col the column index (zero based)
	 * @return the bit (0 or 1)
	 */
	public final int getBit(int col, int row) {
		int bitIdx = getRowIndex(row) + col / 8;
		return (bits[bitIdx] >>> (col % 8)) & 1;
	}
	
	/**
	 * Return a word constructed from biLen bits beginning at position (col, row) in the matrix.
	 * The word is assumed to be packed LSB first
	 * @param row the row index (zero based)
	 * @param col the column index (zero based)
	 * @param bitLen length of the word (up to 64 bits)
	 * @return the word
	 */
	public final long getBits(int col, int row, int bitLen) {
		int bitIdx = getRowIndex(row) + col / 8;
		long retval = 0;
		int colShift = col % 8;
		
		int bitPos = 0;
		int bytePos = 0;
		
		if (colShift > 0) {
			retval = (byte) ((bits[bitIdx] & 0xff) >>> colShift);
			bitPos += (8 - colShift);
			bytePos = 1;
		}
		
		while (bitPos < bitLen) {
			retval |= (((long)bits[bitIdx + bytePos]) & 0xff) << bitPos;
			bitPos += 8;
			++bytePos;
		}
		
		assert(bitLen <= 64);
		// hack: in this case, masking out will zero out the entire return value.
		// this should be handled with arbitrary width integers.
		if (bitLen == 64) {
			return retval;
		}
		// Mask out any extra bits from not being on byte split.
		retval &= (1L << bitLen) - 1;
		
		return retval;
	}
	
	
	/**
	 * Get a bit from the first row (utility function for bit vectors).
	 * @param col
	 */
	public final int getBit(int col) {
		return getBit(col, 0);
	}
	
	/**
	 * Get a several bits from the first row (utility function for bit vectors).
	 * @param col
	 * @param bitLen
	 */
	public final long getBits(int col, int bitLen) {
		return getBits(col, 0, bitLen);
	}
	
	
	
	/**
	 * Set the bit at position (col, row) in the matrix
	 * @param row
	 * @param col
	 * @param bit
	 */
	public final void setBit(int col, int row, int bit) {
        if (copyOnWrite)
            internalCopy();
		bit &= 1;
		int byteIdx = getRowIndex(row) + (col / 8);
		int bitIdx = col % 8;
		bits[byteIdx] = (byte) ((bits[byteIdx] & ~(1 << bitIdx)) | (bit << bitIdx));
	}


	/**
	 * Write a word into the packed bit array. Word is written LSB first.
     * If the word overflows the columns it is truncated.
	 * @param col Starting column
	 * @param row Starting row
	 * @param bitLen length of word in bits
	 * @param word word to write.
	 */
	public final void setBits(int col, int row, int bitLen, long word) {
        if (copyOnWrite)
            internalCopy();

        if (bitLen + col > numCols)
            bitLen = numCols - col;

		int bitIdx = getRowIndex(row) + (col / 8);
 
		int colShift = col % 8;
		if (colShift != 0) {
			int remBits = 8 - colShift;
			if (bitLen < remBits)
				remBits = bitLen;
			int bitMask = (1 << remBits) - 1;
            // Mask out the higher bits.
			bits[bitIdx] &= ~(bitMask << colShift);
			bits[bitIdx] |= (word & bitMask) << colShift;
			word >>>= remBits;
			bitLen -= remBits;
			bitIdx++;
		}
		while (bitLen >= 8) {
			bits[bitIdx] = (byte) (word & 0xff);
			word >>>= 8;
			++bitIdx;
			bitLen -= 8;
		}
		if (bitLen > 0) {
			int mask = (1 << bitLen) - 1; 
			bits[bitIdx] &= ~mask;
			bits[bitIdx] |= (byte) (word & mask); 
		}
	}

    /**
     * Copy all the bits (in row 0) from newBits into this BitMatrix, starting at column col (row 0)
     * @param col
     * @param newBits
     */
    public final void setBits(int col, BitMatrix newBits) {
        copyRow(col, newBits, 0, newBits.getNumCols());
    }


    /**
     * Copy up to maxLen bits from newBits to this BitMatrix, starting at column col (row 0)
     */
    public final void setBits(int col, int maxLen, BitMatrix newBits) {
        copyRow(col, newBits, 0, maxLen);
    }

	/**
	 * Write a word in row 0 (convenience method for bit vectors).
	 * @param col
	 * @param bitLen
	 * @param word
	 */
	public final void setBits(int col, int bitLen, long word) {
		setBits(col, 0, bitLen, word);
	}
	
	/**
	 * Convenience method for treating bit vector 
	 * as a packed array of words.
	 * @param idx
	 * @param wordSize
	 */
	public final long getWord(int idx, int wordSize) {
		return getBits(idx * wordSize, wordSize);
	}
	
	/**
	 * Convenience method for treating bit vector 
	 * as a packed array of words.
	 * @param idx
	 * @param wordSize
	 */
	public final void setWord(int idx, int wordSize, long word) {
		setBits(idx * wordSize, wordSize, word);
	}
	
	/**
	 * Xor the bit at position (col, row) in the matrix
	 * @param row
	 * @param col
	 * @param bit
	 */
	public final void xorBit(int col, int row, int bit) {
		setBit(col, row, getBit(col, row) ^ bit);
	}
	
	/**
	 * Set the bit at pos col in the first row (utility function for bit vectors). 
	 * @param col
	 * @param bit
	 */
	public final void setBit(int col, int bit) {
		setBit(col, 0, bit);
	}
	

	/**
	 * Xor the bit at pos col in the first row (utility function for bit vectors). 
	 * @param col
	 * @param bit
	 */
	public final void xorBit(int col, int bit) {
		xorBit(col, 0, bit);
	}
	
	/**
	 * XOR a row of another matrix with a row of this matrix
	 * @param dstRow the row in this matrix
	 * @param src the other matrix whose row we XORing to this
	 * @param srcRow the row in src to xor.
	 */
	public final void xorRow(int dstRow, BitMatrix src, int srcRow) {
		assert(numCols == src.numCols);
		assert(getUsedBytesPerRow() <= src.getUsedBytesPerRow());

        internalCopy();
		
		int usedBytes = getUsedBytesPerRow();
		int partialByte = getNumCols() % 8;
		int bitMask = (1 << partialByte) - 1;
		if (partialByte != 0)
			--usedBytes;
		
		int startPosA = getRowIndex(dstRow);
		int startPosB = src.getRowIndex(srcRow);
		for (int i = 0; i < usedBytes; ++i)
			bits[startPosA + i] ^= src.bits[startPosB + i];
		if (partialByte != 0) {
			bits[startPosA + usedBytes] ^= src.bits[startPosB + usedBytes] & bitMask;
		}
	}
	
	 /**
	 * Overwrite part of this bitvector with part of another.
	 * If the source  overflows the destination only a prefix will be copied.* @param dstRow the row in this matrix
	 * @param dstStart the first column that will be overwritten
	 * @param src the other matrix whose row we are copying
	 * @param srcStart the first column to read from 
	 * @param srcLen the number of bits to copy.
	 */
	public final void copyRow(int dstStart, BitMatrix src, int srcStart, int srcLen) {
		copyRow(0, dstStart, src, 0, srcStart, srcLen);
	}
	
	
	/**
	 * Copy part of a row from another matrix to part of a row in this matrix.
	 * If the source row overflows the destination row only a prefix will be copied.
	 * @param dstRow the row in this matrix
	 * @param dstStart the first column that will be overwritten
	 * @param src the other matrix whose row we are copying
	 * @param srcRow the row in src to copy.
	 * @param srcStart the first column to read from 
	 * @param srcLen the number of bits to copy.
	 */
	public final void copyRow(int dstRow, int dstStart, BitMatrix src, int srcRow, int srcStart, int srcLen) {
        internalCopy();

		// Truncate if overflow
		if (srcLen + dstStart > numCols)
			srcLen = numCols - dstStart;

		if (((dstStart % 8) | (srcStart % 8) | (srcLen % 8)) == 0) {
			// Everything is cleanly on byte boundaries. Yay!

			int dstPos = getRowIndex(dstRow) + dstStart / 8;
			int srcPos = src.getRowIndex(srcRow) + srcStart / 8;
			System.arraycopy(src.bits, srcPos, bits, dstPos, srcLen / 8);
		} else {
			// Do things inefficiently, bit by bit...
			for (int i = 0; i < srcLen; ++i) {
				setBit(dstStart + i, dstRow, src.getBit(srcStart + i, srcRow));
			}
			
			/*
			 * TODO: Complete efficient implementation that uses arraycopy for the
			 * the middle and bit operations for the edges.
			
			// Compute location of first full byte
			int dstBytePos = getRowIndex(dstRow) + dstStart / 8;
			if (dstStart % 8 != 0)
				++dstBytePos;
			
			int srcFullLen = srcLen - (8 - (dstStart % 8));
			int srcByteLen = srcFullLen / 8;
			
			int srcBytePos = src.getRowIndex(srcRow) + ;
			
			
			System.arraycopy(src.bits, srcPos, bits, dstPos, getUsedBytesPerRow());
			
			// Number of full bytes to copy
			int dstFullLen = srcLen / 8;

			*/

		}
		
	}
	
	/**
	 * Copy a row from another matrix to a row of this matrix.
	 * If the row is longer only a prefix will be copied.
	 * Note: this operation will zeropad the backing array.
	 * 
	 * @param dstRow the row in this matrix
	 * @param src the other matrix whose row we are copying
	 * @param srcRow the row in src to copy.
	 */
	public final void copyRow(int dstRow, BitMatrix src, int srcRow) {
        internalCopy();

		int dstPos = getRowIndex(dstRow);
		int srcPos = src.getRowIndex(srcRow);
        int numBytes = Math.min(getUsedBytesPerRow(), src.getUsedBytesPerRow());
		System.arraycopy(src.bits, srcPos, bits, dstPos, numBytes);
		zeroPad();
	}
	
	/**
	 * Copy the bits from another wrapped bit-vector matrix into this matrix.
	 * Notes: this operation will zero-pad the backing array.
	 *        if src vector is longer, only the prefix will be copied.
	 * @param src the matrix containing the bits to copy.
	 */	
	public final void copyBits(BitMatrix src) {
        if (copyOnWrite)
            internalCopy();

        int numBytes = Math.min(getUsedBytesPerRow(), src.getUsedBytesPerRow());
		System.arraycopy(src.bits, src.getRowIndex(0), bits, getRowIndex(0), numBytes);
		zeroPad();
	}
	
	
	/**
	 * Sets the elements of the bits byte array to zero.
	 */
	public final void reset(){
        if (copyOnWrite) {
            bits = new byte[getNumRows() * getUsedBytesPerRow()];
            rowOffs = 0;
            bytesPerRow = getUsedBytesPerRow();
            copyOnWrite = false;
        } else {
            Arrays.fill(bits, getRowIndex(0), getRowIndex(0) + getUsedBytesPerRow() * getNumRows(), (byte) 0);
        }
        zeroPadded = true;
	}
	
	/**
	 * Xor an entire matrix. The matrix must have at least the same number of rows and columns.
	 * @param b
	 */
	public final void xor(BitMatrix b) {
        internalCopy();

		assert(getNumRows() <= b.getNumRows()) :
                String.format("BitMatrix xor dest is smaller (%dx%d) than source (%dx%d)", getNumCols(),getNumRows(),b.getNumCols(), b.getNumRows());
		assert(getNumCols() <= b.getNumCols()) :
                String.format("BitMatrix xor dest is smaller (%dx%d) than source (%dx%d)", getNumCols(),getNumRows(),b.getNumCols(), b.getNumRows());
		
		int used = getUsedBytesPerRow();
		int partialByte = getNumCols() % 8;

		if (partialByte != 0)
			--used;
		
		for (int i = 0; i < getNumRows(); ++i) {
			int startA = getRowIndex(i);
			int startB = b.getRowIndex(i);
			
			for (int j = 0; j < used; ++j) {
				bits[startA + j] ^= b.bits[startB + j];
			}
			if (partialByte != 0) {
				int bitMask;
				bitMask = (1 << partialByte) - 1;					
				bits[startA + used] ^= b.bits[startB + used] & bitMask;
			}
		}
	}
	
	/**
	 * Transpose the matrix
	 * @return the transpose.
	 */
	public final BitMatrix transpose() {
		// TODO: Improve efficiency.
		BitMatrix T = new BitMatrix(getNumRows(), getNumCols());
		
		for (int i = 0; i < getNumRows(); ++i)
			for (int j = 0; j < getNumCols(); ++j)
				T.setBit(i, j, getBit(j, i));
		
		return T;
	}
	

	@Override
	public void readFrom(SendableInput in) throws IOException {
        internalCopy();

		numCols = in.readInt();
		numRows = in.readInt();
		allocate();
		in.readFully(bits);
	}

	@Override
	public void writeTo(SendableOutput out) throws IOException {
		out.writeInt(numCols);
		out.writeInt(numRows);
		if (bytesPerRow > getUsedBytesPerRow()) {
			// There are extra padding bytes at the end of each row.
			// We'll write out the rows without them
			for (int i = 0; i < numRows; ++i) {
				out.write(bits, getRowIndex(i), getUsedBytesPerRow());
			}
		} else {
			// We can write the entire buffer directly.
			out.write(bits, rowOffs, numRows * bytesPerRow);
		}
	}

    /**
     * Test equality. Two bitmatrices are considered equal if their bits are equal (regardless of the contents
     * of the backing array outside the "real" data.
     *
     * This is an inefficient test at the moment that works bit-by-bit.
     * @param other
     */
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof  BitMatrix))
            return false;

        BitMatrix b = (BitMatrix) other;

        if (numRows != b.numRows || numCols != b.numCols)
            return false;

        for (int row = 0; row < numRows; ++row)
            for (int col = 0; col < numCols; ++col)
                if (getBit(col, row) != b.getBit(col, row))
                    return false;

        return true;
    }

    @Override
    public String toString() {
        return String.format("0x%s[%d%s]", toBigInteger().toString(16), getNumCols(),
                getNumRows() > 1 ? "x" + getNumRows() : "");
    }
}
