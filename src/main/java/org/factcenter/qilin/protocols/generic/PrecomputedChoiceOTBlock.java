package org.factcenter.qilin.protocols.generic;

import org.factcenter.qilin.util.BitMatrix;

/**
 * A class for holding a single block of precomputed OTs (for the chooser).
 * @author talm
 *
 */
class PrecomputedChoiceOTBlock implements PrecomputedOTBlock<PrecomputedChoiceOTBlock> {
	/**
	 * A vector of choice bits (the number of columns must equal the number of rows of {@link #results}. 
	 */
	private BitMatrix choices;

	/**
	 * Offset to first bit in choice column.
	 * (we allow the choices vector to be larger, for cases where we want to "split" blocks without performing copy operations).
	 */
	private int choiceOffs;
	
	/**
	 * A matrix of the results of the precomputed OTs. Row <i>i</i> of the matrix corresponds to a string received
	 * in the OT whose choice bit is given by {@link #choices}[i].
	 */
	private BitMatrix results;
	
	public final BitMatrix getResults() {
		return results;
	}

	public final int getChoiceBit(int i) {
		return choices.getBit(choiceOffs + i);
	}
	
	/**
	 * The number of precomputed OT instances in the block.
	 * @return
	 */
	public final int getNumOTs() {
		return results.getNumRows();
	}

	/**
	 * Return a sub-block (with shared backing matrices) starting at OT offs and containing num OTs
	 * @param offs
	 * @param num
	 * @return
	 */
	public PrecomputedChoiceOTBlock getSubBlock(int offs, int num) {
		return new PrecomputedChoiceOTBlock(choices, choiceOffs + offs, results.getSubMatrix(offs, num));
	}
	
	/**
	 * Remove the first num OTs from this block and return them as a new subblock (this block will be modified as well).
	 * @param num
	 * @return
	 */
	public PrecomputedChoiceOTBlock remove(int num) {
		PrecomputedChoiceOTBlock retval = getSubBlock(0, num);
		choiceOffs += num;
		results.subrows(num, getNumOTs() - num);
		return retval;
	}
	
	public PrecomputedChoiceOTBlock(BitMatrix choiceBits, int choiceOffs, BitMatrix resultBits) {
		this.choices = choiceBits;
		this.choiceOffs = choiceOffs;
		this.results = resultBits;
	}
	
	public PrecomputedChoiceOTBlock(BitMatrix choiceBits, BitMatrix resultBits) {
		this(choiceBits, 0, resultBits);
	}
}

