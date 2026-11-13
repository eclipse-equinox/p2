/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.util;

import java.text.SimpleDateFormat;
import java.util.*;
import org.eclipse.ecf.internal.core.identity.Activator;
import org.eclipse.osgi.service.debug.DebugOptions;

/**
 * A utility for tracing debug information. Provides a simple interface for
 * filtering and generating trace output.
 * 
 */
public class Trace {

	private static final boolean TRACEALL = Boolean
			.getBoolean(System.getProperty("org.eclipse.ecf.core.util.traceAll", "false"));

	private static final List<String> TRACE_BUNDLES = new ArrayList<String>();

	static {
		if (!TRACEALL) {
			String tracePlugins = System.getProperty("org.eclipse.ecf.core.util.traceBundles");
			if (tracePlugins != null)
				try {
					String[] plugins = tracePlugins.split(",");
					TRACE_BUNDLES.addAll(Arrays.asList(plugins));
				} catch (Exception e) {
					System.err.println("Unexpected exception in Trace class static initializer");
					e.printStackTrace(System.err);
				}
		}
	}

	/**
	 * private constructor for the static class.
	 */
	private Trace() {
		super();
	}

	/**
	 * String containing an open parenthesis.
	 * 
	 */
	protected static final String PARENTHESIS_OPEN = "("; //$NON-NLS-1$

	/**
	 * String containing a close parenthesis.
	 * 
	 */
	protected static final String PARENTHESIS_CLOSE = ")"; //$NON-NLS-1$

	/**
	 * String containing TRACE
	 * 
	 */
	protected static final String TRACE_STR = "TRACE"; //$NON-NLS-1$

	/**
	 * Prefix for tracing the changing of values.
	 * 
	 */
	protected static final String PREFIX_TRACING = "TRACING "; //$NON-NLS-1$

	/**
	 * Prefix for tracing the changing of values.
	 * 
	 */
	protected static final String PREFIX_CHANGING = "CHANGING "; //$NON-NLS-1$

	/**
	 * Prefix for tracing the catching of throwables.
	 * 
	 */
	protected static final String PREFIX_CATCHING = "CAUGHT "; //$NON-NLS-1$

	/**
	 * Prefix for tracing the throwing of throwables.
	 * 
	 */
	protected static final String PREFIX_THROWING = "THROWN "; //$NON-NLS-1$

	/**
	 * Prefix for tracing the entering of methods.
	 * 
	 */
	protected static final String PREFIX_ENTERING = "ENTERING "; //$NON-NLS-1$

	/**
	 * Prefix for tracing the exiting of methods.
	 * 
	 */
	protected static final String PREFIX_EXITING = "EXITING "; //$NON-NLS-1$

	/**
	 * Separator for methods.
	 * 
	 */
	protected static final String SEPARATOR_METHOD = "#"; //$NON-NLS-1$

	/**
	 * Separator for parameters.
	 * 
	 */
	protected static final String SEPARATOR_PARAMETER = ", "; //$NON-NLS-1$

	/**
	 * Separator for return values.
	 * 
	 */
	protected static final String SEPARATOR_RETURN = ":"; //$NON-NLS-1$

	/**
	 * Separator containing a space.
	 * 
	 */
	protected static final String SEPARATOR_SPACE = " "; //$NON-NLS-1$

	/**
	 * Label indicating old value.
	 * 
	 */
	protected static final String LABEL_OLD_VALUE = "old="; //$NON-NLS-1$

	/**
	 * Label indicating new value.
	 * 
	 */
	protected static final String LABEL_NEW_VALUE = "new="; //$NON-NLS-1$

	/**
	 * The cached debug options (for optimization).
	 */
	private static final Map<String, Boolean> cachedOptions = new HashMap<String, Boolean>();

	/**
	 * Retrieves a Boolean value indicating whether tracing is enabled for the
	 * specified plug-in.
	 * 
	 * @return Whether tracing is enabled for the plug-in.
	 * @param pluginId The symbolic plugin id for which to determine trace
	 * enablement.
	 * 
	 */
	protected static boolean shouldTrace(String pluginId) {
		return shouldTrace0(pluginId + "/debug"); //$NON-NLS-1$
	}

	protected static boolean shouldTrace0(String option) {
		if (option == null)
			return false;
		Activator activator = Activator.getDefault();
		if (activator == null)
			return false;
		DebugOptions debugOptions = activator.getDebugOptions();
		if (debugOptions == null)
			return false;
		String result = debugOptions.getOption(option);
		return (result == null) ? false : result.equalsIgnoreCase("true"); //$NON-NLS-1$
	}

	/**
	 * Retrieves a Boolean value indicating whether tracing is enabled for the
	 * specified debug option of the specified plug-in.
	 * 
	 * @return Whether tracing is enabled for the debug option of the plug-in.
	 * @param pluginId The plug-in for which to determine trace enablement.
	 * @param option The debug option for which to determine trace enablement.
	 * 
	 */
	public static boolean shouldTrace(String pluginId, String option) {
		if (pluginId == null)
			return false;
		if (TRACEALL || TRACE_BUNDLES.contains(pluginId))
			return true;
		if (shouldTrace(pluginId)) {
			Boolean value = null;

			synchronized (cachedOptions) {
				value = cachedOptions.get(option);
				if (null == value) {
					value = shouldTrace0(option);
					cachedOptions.put(option, value);
				}
			}

			return value.booleanValue();
		}

		return false;
	}

	/**
	 * Retrieves a textual representation of the specified argument.
	 * 
	 * @return A textual representation of the specified argument.
	 * @param argument The argument for which to retrieve a textual
	 * representation.
	 * 
	 */
	public static String getArgumentString(Object argument) {
		if (argument == null)
			return "null"; //$NON-NLS-1$
		if (argument instanceof byte[])
			return getStringFromByteArray((byte[]) argument);
		if (argument.getClass().isArray())
			return getArgumentsString((Object[]) argument);
		return String.valueOf(argument);
	}

	private static String getStringFromByteArray(byte[] bytes) {
		StringBuilder buf = new StringBuilder("["); //$NON-NLS-1$
		for (int i = 0; i < bytes.length; i++) {
			buf.append(bytes[i]);
			if (i == (bytes.length - 1))
				buf.append("]"); //$NON-NLS-1$
			else
				buf.append(","); //$NON-NLS-1$
		}
		return buf.toString();
	}

	/**
	 * Retrieves a textual representation of the specified arguments.
	 * 
	 * @return A textual representation of the specified arguments.
	 * @param arguments The arguments for which to retrieve a textual
	 * representation.
	 * 
	 */
	public static String getArgumentsString(Object[] arguments) {
		if (arguments == null)
			return "[]"; //$NON-NLS-1$
		StringBuilder buffer = new StringBuilder("["); //$NON-NLS-1$

		for (int i = 0; i < arguments.length; i++) {
			buffer.append(getArgumentString(arguments[i]));

			if (i < arguments.length - 1)
				buffer.append(SEPARATOR_PARAMETER);
		}
		buffer.append("]"); //$NON-NLS-1$
		return buffer.toString();
	}

	/**
	 * Traces the specified message.
	 * 
	 * @param message The message to be traced.
	 * 
	 */
	protected static void trace(String message) {
		StringBuilder buf = new StringBuilder(PARENTHESIS_OPEN);
		buf.append(TRACE_STR).append(PARENTHESIS_CLOSE).append(getTimeString()).append(message).append(SEPARATOR_SPACE);
		System.out.println(buf.toString());
	}

	/**
	 * Get date and time string
	 * 
	 * @return String with current date and time
	 */
	protected static String getTimeString() {
		Date d = new Date();
		SimpleDateFormat df = new SimpleDateFormat("[MM/dd/yy;HH:mm:ss:SSS]"); //$NON-NLS-1$
		return df.format(d);
	}

	/**
	 * Traces the specified message from the specified plug-in.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param message The message to be traced.
	 * 
	 */
	public static void trace(String pluginId, String message) {
		if (shouldTrace(pluginId))
			trace(message);
	}

	/**
	 * Traces the specified message from the specified plug-in for the specified
	 * debug option.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param message The message to be traced.
	 * 
	 */
	public static void trace(String pluginId, String option, String message) {
		if (shouldTrace(pluginId, option))
			trace(message);
	}

	/**
	 * Traces the specified message from the specified plug-in for the specified
	 * debug option.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param clazz The class whose method is being entered.
	 * @param methodName The name of method that is being entered.
	 * @param message The message to be traced.
	 * 
	 */
	public static void trace(String pluginId, String option, @SuppressWarnings("rawtypes") Class clazz,
			String methodName, String message) {
		if (shouldTrace(pluginId, option)) {
			StringBuffer buf = new StringBuffer(PREFIX_TRACING).append(clazz.getName());
			buf.append(SEPARATOR_METHOD).append(methodName);
			buf.append(PARENTHESIS_OPEN).append(message).append(PARENTHESIS_CLOSE);
			trace(buf.toString());
		}
	}

	/**
	 * Traces the changing of a value.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param valueDescription The description of the value which is changing.
	 * @param oldValue The old value.
	 * @param newValue The new value.
	 */
	public static void changing(String pluginId, String option, String valueDescription, Object oldValue,
			Object newValue) {

		if (shouldTrace(pluginId, option)) {
			StringBuilder buf = new StringBuilder(PREFIX_CHANGING);
			buf.append(valueDescription).append(SEPARATOR_SPACE).append(LABEL_OLD_VALUE)
					.append(getArgumentString(oldValue));
			buf.append(SEPARATOR_PARAMETER).append(LABEL_NEW_VALUE).append(getArgumentString(newValue));
			trace(buf.toString());
		}
	}

	/**
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param clazz The class in which the value is changing.
	 * @param methodName The name of the method in which the value is changing.
	 * @param valueDescription The description of the value which is changing.
	 * @param oldValue The old value.
	 * @param newValue The new value.
	 */
	public static void changing(String pluginId, String option, @SuppressWarnings("rawtypes") Class clazz,
			String methodName, String valueDescription, Object oldValue, Object newValue) {
		if (shouldTrace(pluginId, option)) {
			StringBuilder buf = new StringBuilder(PREFIX_CHANGING);
			buf.append(valueDescription).append(SEPARATOR_SPACE).append(LABEL_OLD_VALUE)
					.append(getArgumentString(oldValue));
			buf.append(SEPARATOR_PARAMETER).append(LABEL_NEW_VALUE).append(getArgumentString(newValue));
			buf.append(SEPARATOR_SPACE).append(PARENTHESIS_OPEN).append(clazz.getName()).append(SEPARATOR_METHOD);
			buf.append(methodName).append(PARENTHESIS_CLOSE);
			trace(buf.toString());
		}
	}

	/**
	 * Traces the catching of the specified throwable in the specified method of
	 * the specified class.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param clazz The class in which the throwable is being caught.
	 * @param methodName The name of the method in which the throwable is being
	 * caught.
	 * @param throwable The throwable that is being caught.
	 * 
	 */
	public static void catching(String pluginId, String option, @SuppressWarnings("rawtypes") Class clazz,
			String methodName, Throwable throwable) {

		if (shouldTrace(pluginId, option)) {
			StringBuilder buf = new StringBuilder(PREFIX_CATCHING);
			if (throwable != null) {
				String message = throwable.getMessage();
				if (message != null)
					buf.append(message).append(SEPARATOR_SPACE);
			}
			buf.append(PARENTHESIS_OPEN).append(clazz.getName()).append(SEPARATOR_METHOD);
			buf.append(methodName).append(PARENTHESIS_CLOSE);
			trace(buf.toString());
			if (throwable != null) {
				throwable.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Traces the throwing of the specified throwable from the specified method
	 * of the specified class.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param clazz The class from which the throwable is being thrown.
	 * @param methodName The name of the method from which the throwable is
	 * being thrown.
	 * @param throwable The throwable that is being thrown.
	 * 
	 */
	public static void throwing(String pluginId, String option, @SuppressWarnings("rawtypes") Class clazz,
			String methodName, Throwable throwable) {

		if (shouldTrace(pluginId, option)) {
			StringBuilder buf = new StringBuilder(PREFIX_THROWING);
			if (throwable != null) {
				String message = throwable.getMessage();
				if (message != null)
					buf.append(message).append(SEPARATOR_SPACE);
			}
			buf.append(PARENTHESIS_OPEN).append(clazz.getName()).append(SEPARATOR_METHOD);
			buf.append(methodName).append(PARENTHESIS_CLOSE);
			trace(buf.toString());
			throwable.printStackTrace(System.err);
		}
	}

	/**
	 * Traces the entering into the specified method of the specified class.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param clazz The class whose method is being entered.
	 * @param methodName The name of method that is being entered.
	 * 
	 */
	public static void entering(String pluginId, String option, @SuppressWarnings("rawtypes") Class clazz,
			String methodName) {

		if (shouldTrace(pluginId, option)) {
			StringBuffer buf = new StringBuffer(PREFIX_ENTERING).append(clazz.getName());
			buf.append(SEPARATOR_METHOD).append(methodName).append(PARENTHESIS_OPEN).append(PARENTHESIS_CLOSE);
			trace(buf.toString());
		}
	}

	/**
	 * Traces the entering into the specified method of the specified class,
	 * with the specified parameter.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param clazz The class whose method is being entered.
	 * @param methodName The name of method that is being entered.
	 * @param parameter The parameter to the method being entered.
	 * 
	 */
	public static void entering(String pluginId, String option, @SuppressWarnings("rawtypes") Class clazz,
			String methodName, Object parameter) {

		if (shouldTrace(pluginId, option)) {
			StringBuffer buf = new StringBuffer(PREFIX_ENTERING).append(clazz.getName());
			buf.append(SEPARATOR_METHOD).append(methodName);
			buf.append(PARENTHESIS_OPEN).append(getArgumentString(parameter)).append(PARENTHESIS_CLOSE);
			trace(buf.toString());
		}

	}

	/**
	 * Traces the entering into the specified method of the specified class,
	 * with the specified parameters.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param clazz The class whose method is being entered.
	 * @param methodName The name of method that is being entered.
	 * @param parameters The parameters to the method being entered.
	 * 
	 */
	public static void entering(String pluginId, String option, @SuppressWarnings("rawtypes") Class clazz,
			String methodName, Object[] parameters) {

		if (shouldTrace(pluginId, option)) {
			StringBuffer buf = new StringBuffer(PREFIX_ENTERING).append(clazz.getName());
			buf.append(SEPARATOR_METHOD).append(methodName);
			buf.append(PARENTHESIS_OPEN).append(getArgumentString(parameters)).append(PARENTHESIS_CLOSE);
			trace(buf.toString());
		}

	}

	/**
	 * Traces the exiting from the specified method of the specified class.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param clazz The class whose method is being exited.
	 * @param methodName The name of method that is being exited.
	 * 
	 */
	public static void exiting(String pluginId, String option, @SuppressWarnings("rawtypes") Class clazz,
			String methodName) {

		if (shouldTrace(pluginId, option)) {
			StringBuffer buf = new StringBuffer(PREFIX_EXITING).append(clazz.getName());
			buf.append(SEPARATOR_METHOD).append(methodName);
			trace(buf.toString());
		}
	}

	/**
	 * Traces the exiting from the specified method of the specified class, with
	 * the specified return value.
	 * 
	 * @param pluginId The plug-in from which to trace.
	 * @param option The debug option for which to trace.
	 * @param clazz The class whose method is being exited.
	 * @param methodName The name of method that is being exited.
	 * @param returnValue The return value of the method being exited.
	 * 
	 */
	public static void exiting(String pluginId, String option, @SuppressWarnings("rawtypes") Class clazz,
			String methodName, Object returnValue) {

		if (shouldTrace(pluginId, option)) {
			StringBuffer buf = new StringBuffer(PREFIX_EXITING).append(clazz.getName());
			buf.append(SEPARATOR_METHOD).append(methodName);
			buf.append(PARENTHESIS_OPEN).append(getArgumentString(returnValue)).append(PARENTHESIS_CLOSE);
			trace(buf.toString());
		}

	}

}
