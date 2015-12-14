package org.factcenter.qilin.util;

import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.*;

public class BitMatrixTest {
	
	Random rand = new Random(0);

	@Test
	public void testTranspose() {
		int cols = 1000;
		int rows = 500;
		
		BitMatrix A = new BitMatrix(cols,rows);
		A.fillRandom(rand);
		
		BitMatrix B = A.transpose();
		
		for (int col = 0; col < cols; ++col) {
			for (int row = 0; row < rows; ++row) {
				assertEquals(A.getBit(col, row), B.getBit(row, col));
			}
		}
	}
	

	@Test
	public void testSubcolumnsByteAligned() {
		int cols = 1000;
		int rows = 500;
		
		BitMatrix A = new BitMatrix(cols,rows);
		A.fillRandom(rand);
		
		BitMatrix B = A.clone();
		
		B.subcolumns(32, 200);
		
		for (int col = 32; col < 200; ++col) {
			for (int row = 0; row < rows; ++row) {
				assertEquals("("+col+","+row+")", A.getBit(col, row), B.getBit(col - 32, row));
			}
		}
		
	}		

	@Test
	public void testSubcolumns() {
		int cols = 1000;
		int rows = 300;
		
		int subStart = 47;
		int subNum = 100;
		
		BitMatrix A = new BitMatrix(cols,rows);
		A.fillRandom(rand);
		
		BitMatrix B = A.clone();
		
		B.subcolumns(subStart, subNum);
		
		for (int col = 0; col < subNum; ++col) {
			for (int row = 0; row < rows; ++row) {
				assertEquals("("+col+","+row+")", A.getBit(col + subStart, row), B.getBit(col, row));
			}
		}
	}
	
	@Test
	public void testCopyRow() {
		int cols = 82;
		int rows = 20;
		int half = rows/2;
		
		BitMatrix bSrc1 = new BitMatrix(cols, half);
		bSrc1.fillRandom(rand);
		
		BitMatrix bSrc2 = new BitMatrix(cols, half);
		bSrc2.fillRandom(rand);
		
		BitMatrix bDst = new BitMatrix(cols, rows);
		
		for (int i = 0; i < half; ++i) {
			bDst.copyRow(i, bSrc1, i);
			bDst.copyRow(i+half, bSrc2, i);
		}
		
		for (int row = 0; row < half; ++row) {
			for (int col = 0; col < cols; ++col) {
				assertEquals("1("+col+","+row+")", bSrc1.getBit(col, row), bDst.getBit(col, row));
				assertEquals("2("+col+","+row+")", bSrc2.getBit(col, row), bDst.getBit(col, row + half));
			}
			
			int len = bDst.getUsedBytesPerRow();
			byte[] dstBytes = bDst.getBackingArray();
			byte[] src1Bytes = bSrc1.getBackingArray();
			byte[] src2Bytes = bSrc2.getBackingArray();
			for (int i = 0; i < len; ++i) {
				assertEquals("1(i="+i+","+row+")", src1Bytes[bSrc1.getRowIndex(row) + i], dstBytes[bDst.getRowIndex(row) + i]);
				assertEquals("2(i="+i+","+row+")", src2Bytes[bSrc2.getRowIndex(row) + i], dstBytes[bDst.getRowIndex(row + half) + i]);
			}
		}
	}
	

	@Test
	public void testSubmatrix() {
		int cols = 1000;
		int rows = 500;
		
		BitMatrix A = new BitMatrix(cols,rows);
		A.fillRandom(rand);
		
		BitMatrix B = A.getSubMatrix(200, 250);
		
		for (int col = 0; col < cols; ++col) {
			for (int row = 200; row < 250; ++row) {
				assertEquals("("+col+","+row+")",A.getBit(col, row), B.getBit(col, row - 200));
			}
		}
		
	}		
	

	@Test
	public void testSetBitsUnaligned() {
		BitMatrix bm = new BitMatrix(40);
		
		bm.setBits(2, 17, 0x3b2a1);
		byte[] arr = bm.getBackingArray();
		byte[] expected = {(byte) 0x84, (byte)0xca, 0x06, 0, 0};
		
		assertArrayEquals(expected, arr);
		
		bm.setBits(5, 2, 0x3);
		expected[0] = (byte) 0xe4;
		assertArrayEquals(expected, arr);
		
		bm.setBits(12, 8, 0x93);
		expected[1] = 0x3a;
		expected[2] = 0x09;
		assertArrayEquals(expected, arr);
	}
	
	@Test
	public void testGetSetBitsRandomized() {
		ArrayList<Integer> skips = new ArrayList<Integer>();
		ArrayList<Long> vals = new ArrayList<Long>();
		byte[] rom = new byte[1000];
		int bitlen = rom.length * 8;
		BitMatrix bm = new BitMatrix(rom, 0);

		int i = 0;
		while (i < bitlen) {
			int maxlen = (bitlen - i) > 15 ? 15 : (bitlen - i);
			int len = rand.nextInt(maxlen) + 1;
			long val = rand.nextLong();
			skips.add(len);
			vals.add(val);
			bm.setBits(i, len, val);
			i += len;
		}

		i = 0;
		for (int idx = 0; idx < skips.size(); ++idx) {
			int len = skips.get(idx);
			Long val = vals.get(idx);
			long testVal = bm.getBits(i, len);
			int mask = (1 << len) - 1;
			assertEquals(String.format("Index %d, start=%d,len=%d", idx, i, len), val & mask,
					testVal & mask);

			i += len;
		}

	}

	@Test
    public void testToInteger() {

        // Small integer
        BitMatrix bm = new BitMatrix(3);

        bm.bits[0] = (byte) 0xae;

        assertEquals(6L, bm.toInteger(3));
        assertEquals(6L, bm.toInteger(8));
        assertEquals(2L, bm.toInteger(2));


        // Byte-sized
        bm = new BitMatrix(8);
        bm.bits[0] = (byte) 0xae;

        assertEquals(6L, bm.toInteger(3));
        assertEquals(0xaeL, bm.toInteger(8));
        assertEquals(2L, bm.toInteger(2));

        // Long
        bm = new BitMatrix(64);
        int[] testbits = { 0xae, 0xea, 0x12, 0x55, 0x34, 0x78, 0x87, 0xcb };
        for (int i = 0; i < bm.bits.length; ++i)
            bm.bits[i] = (byte) testbits[i];

        assertEquals(6L, bm.toInteger(3));
        assertEquals(0xaeL, bm.toInteger(8));
        assertEquals(0xeaaeL, bm.toInteger(16));
        assertEquals(0x12eaaeL, bm.toInteger(24));
        assertEquals(0xcb8778345512eaaeL, bm.toInteger(64));
        assertEquals(0x2aeL, bm.toInteger(10));
        assertEquals(0x2eaaeL, bm.toInteger(20));

        // Longer
        bm = new BitMatrix(128);
        for (int i = 0; i < testbits.length; ++i)
            bm.bits[i] = (byte) testbits[i];

        assertEquals(6L, bm.toInteger(3));
        assertEquals(0xaeL, bm.toInteger(8));
        assertEquals(0xeaaeL, bm.toInteger(16));
        assertEquals(0x12eaaeL, bm.toInteger(24));
        assertEquals(0xcb8778345512eaaeL, bm.toInteger(64));
        assertEquals(0x2aeL, bm.toInteger(10));
        assertEquals(0x2eaaeL, bm.toInteger(20));

    }

    @Test
    public void testToBigInteger() {

        // Small integer
        BitMatrix bm = new BitMatrix(3);

        bm.bits[0] = (byte) 0xae;

        assertEquals(6L, bm.toBigInteger(3).longValue());
        assertEquals(6L, bm.toBigInteger(8).longValue());
        assertEquals(2L, bm.toBigInteger(2).longValue());

        // Byte-sized
        bm = new BitMatrix(8);
        bm.bits[0] = (byte) 0xae;

        assertEquals(6L, bm.toBigInteger(3).longValue());
        assertEquals(0xaeL, bm.toBigInteger(8).longValue());
        assertEquals(2L, bm.toBigInteger(2).longValue());

        // Long
        bm = new BitMatrix(64);
        int[] testbits = { 0xae, 0xea, 0x12, 0x55, 0x34, 0x78, 0x87, 0xcb };
        for (int i = 0; i < bm.bits.length; ++i)
            bm.bits[i] = (byte) testbits[i];

        assertEquals(6L, bm.toBigInteger(3).longValue());
        assertEquals(0xaeL, bm.toBigInteger(8).longValue());
        assertEquals(0xeaaeL, bm.toBigInteger(16).longValue());
        assertEquals(0x12eaaeL, bm.toBigInteger(24).longValue());
        assertEquals(0xcb8778345512eaaeL, bm.toBigInteger(64).longValue());
        assertEquals(0x2aeL, bm.toBigInteger(10).longValue());
        assertEquals(0x2eaaeL, bm.toBigInteger(20).longValue());

        // Longer
        bm = new BitMatrix(128);
        rand.nextBytes(bm.bits);

        BigInteger val = BigInteger.valueOf(0);

        int width = 120;
        for (int i = width - 1; i >= 0; --i) {
            val = val.shiftLeft(1);
            if (bm.getBit(i) != 0)
                val = val.or(BigInteger.ONE);
        }

        assertEquals(val, bm.toBigInteger(width));
    }


    @Test
    public void testValueOfInteger() {
        for (int i = 0; i < 100; ++i) {
            long val = rand.nextLong();

            int wid = rand.nextInt(64) + 1;

            BitMatrix bm = BitMatrix.valueOf(val, wid);

            long result = bm.toInteger(64);
            if (wid < 64)
                val &= ((1L << wid) - 1);
            assertEquals(String.format("Failed on try %d (wid=%d)", i, wid), val, result);
        }
    }


    @Test
    public void testValueOfBigInteger() {
        byte[] bigbits = new byte[16];
        for (int i = 0; i < 100; ++i) {
            rand.nextBytes(bigbits);
            BigInteger val = new BigInteger(1, bigbits);

            int wid = rand.nextInt(bigbits.length * 8) + 1;

            BitMatrix bm = BitMatrix.valueOf(val, wid);

            BigInteger result = bm.toBigInteger(wid);
            BigInteger mask = BigInteger.ONE.shiftLeft(wid).subtract(BigInteger.ONE);
            val = val.and(mask);
            assertEquals(String.format("Failed on try %d (wid=%d)", i, wid), val, result);
        }
    }

    @Test
    public void testCopyOnWrite() {
        BitMatrix bm1 = new BitMatrix(100);
        bm1.fillRandom(rand);

        BitMatrix bm2 = bm1.getSubMatrixCols(16, 64);

        bm1.copyOnWrite = false;

        bm1.xorBit(30, 1);

        assertEquals(bm1.getBit(30), bm2.getBit(30));

        bm1.copyOnWrite = true;

        bm1.xorBit(30, 1);

        assertNotEquals(bm1.getBit(30), bm2.getBit(30));
    }
}
