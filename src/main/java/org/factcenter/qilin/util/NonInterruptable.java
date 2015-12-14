package org.factcenter.qilin.util;

import java.util.concurrent.BlockingQueue;

/**
 * Utility for making {@link BlockingQueue}s non-interruptable by catching and ignoring interrupts.
 * This makes things simpler to write, but is probably bad design.
 * 
 * @author talm
 *
 */
public class NonInterruptable {
	public static <T> void put(BlockingQueue<T> queue, T element) {
		while(true) {
			try {
				queue.put(element);
				break;
			} catch (InterruptedException ie) {
				// Ignore interrupts, but reassert the thread's interrupt status
				Thread.currentThread().interrupt();
			}
		}
	}
	
	public static <T> T take(BlockingQueue<T> queue) {
		while(true) {
			try {
				return queue.take();
			} catch (InterruptedException ie) {
				// Ignore interrupts, but reassert the thread's interrupt status
				Thread.currentThread().interrupt();
			}
		}
	}
}
