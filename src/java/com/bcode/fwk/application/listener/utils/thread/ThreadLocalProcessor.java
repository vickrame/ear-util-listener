/**
 * 
 */
package com.bcode.fwk.application.listener.utils.thread;

import java.lang.ref.Reference;

/**
 * 
 * @author vickrame
 *
 */
public interface ThreadLocalProcessor {
	void process(Thread thread, Reference entry, ThreadLocal<?> threadLocal,
			Object value);
}
