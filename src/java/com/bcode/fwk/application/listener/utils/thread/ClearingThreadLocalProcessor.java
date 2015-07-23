/**
 * 
 */
package com.bcode.fwk.application.listener.utils.thread;

import java.lang.ref.Reference;
import java.lang.reflect.Field;

import com.bcode.fwk.application.listener.utils.ClassLoaderUtils;

/**
 * 
 * @author vickrame
 *
 */
public class ClearingThreadLocalProcessor extends WarningThreadLocalProcessor {

	protected final Field java_lang_Thread_threadLocals;

	protected final Field java_lang_Thread_inheritableThreadLocals;

	protected final Field java_lang_ThreadLocal$ThreadLocalMap_table;

	protected Field java_lang_ThreadLocal$ThreadLocalMap$Entry_value;

	public ClearingThreadLocalProcessor(String typeApp) {
		super(typeApp);
		java_lang_Thread_threadLocals = ClassLoaderUtils.findField(
				Thread.class, "threadLocals");
		java_lang_Thread_inheritableThreadLocals = ClassLoaderUtils.findField(
				Thread.class, "inheritableThreadLocals");
		java_lang_ThreadLocal$ThreadLocalMap_table = ClassLoaderUtils
				.findFieldOfClass("java.lang.ThreadLocal$ThreadLocalMap",
						"table");

		// if (java_lang_Thread_threadLocals == null)
		// System.err.println("java.lang.Thread.threadLocals not found; something is seriously wrong!");
		//
		// if (java_lang_Thread_inheritableThreadLocals == null)
		// System.err.println("java.lang.Thread.inheritableThreadLocals not found; something is seriously wrong!");
		//
		// if (java_lang_ThreadLocal$ThreadLocalMap_table == null)
		// System.err.println("java.lang.ThreadLocal$ThreadLocalMap.table not found; something is seriously wrong!");

	}

	public void processFurther(Thread thread, Reference entry,
			ThreadLocal<?> threadLocal, Object value) {
		if (threadLocal != null && thread == Thread.currentThread()) { // If
																		// running
																		// for
																		// current
																		// thread
																		// and
																		// we
																		// have
																		// the
																		// ThreadLocal
																		// ...
			// ... remove properly
			threadLocal.remove();
		} else { // We cannot remove entry properly, so just make it stale
			entry.clear(); // Clear the key

			if (java_lang_ThreadLocal$ThreadLocalMap$Entry_value == null) {
				java_lang_ThreadLocal$ThreadLocalMap$Entry_value = ClassLoaderUtils
						.findField(entry.getClass(), "value");
			}

			try {
				java_lang_ThreadLocal$ThreadLocalMap$Entry_value.set(entry,
						null); // Clear value to avoid circular references
			} catch (IllegalAccessException iaex) {
				// System.err.println(iaex);
				// iaex.printStackTrace();
			}
		}
	}
}
