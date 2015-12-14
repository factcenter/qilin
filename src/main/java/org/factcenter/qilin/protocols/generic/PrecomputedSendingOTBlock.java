package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.util.BitMatrix;

/**
 * A class for holding a single block of precomputed OTs (for the sender).
 * @author talm
 *
 */
class PrecomputedSendingOTBlock implements PrecomputedOTBlock<PrecomputedSendingOTBlock> {
	/**
	 * A matrix consisting of the strings sent for 0 choice bits (each row is a string).
	 */
	private BitMatrix x0;

	/**
	 * A matrix consisting of the strings sent for 1 choice bits (each row is a string).
	 */
	private BitMatrix x1;


	public final BitMatrix getX(int i) {
		return (i == 0) ? x0 : x1;
	}
	
	/**
	 * The number of precomputed OT instances in the block.
	 * @return
	 */
	public final int getNumOTs() {
		if (x0 == null)
			return 0;
		return x0.getNumRows();
	}

	/**
	 * Return a sub-block (with shared backing matrices) starting at OT offs and containing num OTs
	 * @param offs
	 * @param num
	 * @return
	 */
	public PrecomputedSendingOTBlock getSubBlock(int offs, int num) {
		return new PrecomputedSendingOTBlock(x0.getSubMatrix(offs, num), x1.getSubMatrix(offs, num));
	}
	
	/**
	 * Remove the first num OTs from this block and return them as a new subblock (this block will be modified as well).
	 * @param num
	 * @return
	 */
	public PrecomputedSendingOTBlock remove(int num) {
		PrecomputedSendingOTBlock retval = getSubBlock(0, num);
		int newSize = getNumOTs() - num;
		x0.subrows(num, newSize);
		x1.subrows(num, newSize);
		return retval;
	}
	
	
	public PrecomputedSendingOTBlock(BitMatrix x0, BitMatrix x1) {
		this.x0 = x0;
		this.x1 = x1;
	}
}