/**
 * 
 */
package com.bcode.fwk.application.listener.utils.thread;

import java.lang.ref.Reference;

import com.bcode.fwk.application.listener.utils.ClassLoaderUtils;

/**
 * 
 * @author vickrame
 *
 */
public class WarningThreadLocalProcessor implements ThreadLocalProcessor {

	protected String typeApp;

	public WarningThreadLocalProcessor(String typeApp) {
		this.typeApp = typeApp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.socgen.fwk.application.listener.utils.thread.ThreadLocalProcessor
	 * #process(java.lang.Thread, java.lang.ref.Reference,
	 * java.lang.ThreadLocal, java.lang.Object)
	 */
	@Override
	public final void process(Thread thread, Reference entry,
			ThreadLocal<?> threadLocal, Object value) {

		final boolean customThreadLocal = typeApp.equals("ear") ? ClassLoaderUtils
				.isLoadedInEarApplication(threadLocal) : ClassLoaderUtils
				.isLoadedInWebApplication(threadLocal);
		final boolean valueLoadedInWebApp = typeApp.equals("ear") ? ClassLoaderUtils
				.isLoadedInEarApplication(value) : ClassLoaderUtils
				.isLoadedInWebApplication(value);
		final boolean valueCLInAppl = typeApp.equals("ear") ? ClassLoaderUtils
				.getValueIsEarAppClassLoaderOrChild((ClassLoader) value)
				: ClassLoaderUtils
						.getValueIsWebAppClassLoaderOrChild((ClassLoader) value);

		if (customThreadLocal || valueLoadedInWebApp
				|| (value instanceof ClassLoader && valueCLInAppl)) {

			// This ThreadLocal is either itself loaded by the web app
			// classloader, or it's value is
			// Let's do something about it

			StringBuilder message = new StringBuilder();
			if (threadLocal != null) {
				if (customThreadLocal) {
					message.append("Custom ");
				}
				message.append("ThreadLocal of type ")
						.append(threadLocal.getClass().getName()).append(": ")
						.append(threadLocal);
			} else {
				message.append("Unknown ThreadLocal");
			}
			message.append(" with value ").append(value);
			if (value != null) {
				message.append(" of type ").append(value.getClass().getName());
				if (valueLoadedInWebApp)
					message.append(" that is loaded by web app");
			}
			processFurther(thread, entry, threadLocal, value); // Allow
																// subclasses to
																// perform
																// further
																// processing
		}
	}

	/**
	 * After having detected potential ThreadLocal leak and warned about it,
	 * this method is called. Subclasses may override this method to perform
	 * further processing, such as clean up.
	 */
	protected void processFurther(Thread thread, Reference entry,
			ThreadLocal<?> threadLocal, Object value) {
		// To be overridden in subclass
	}

}
