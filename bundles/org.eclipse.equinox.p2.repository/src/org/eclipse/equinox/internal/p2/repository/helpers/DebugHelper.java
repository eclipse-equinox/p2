/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository.helpers;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.osgi.service.debug.DebugOptions;

public class DebugHelper {
	public static final String LINE_SEPARATOR = System.lineSeparator();

	public static final boolean DEBUG_REPOSITORY_CREDENTIALS;
	public static final boolean DEBUG_REPOSITORY_TRANSPORT;
	public static final boolean DEBUG_KEY_SERVICE;

	static {
		DebugOptions options = ServiceHelper.getService(Activator.getContext(), DebugOptions.class);
		if (options != null) {
			DEBUG_REPOSITORY_CREDENTIALS = options.getBooleanOption(Activator.ID + "/credentials/debug", false); //$NON-NLS-1$
			DEBUG_REPOSITORY_TRANSPORT = options.getBooleanOption(Activator.ID + "/transport/debug", false); //$NON-NLS-1$
			DEBUG_KEY_SERVICE = options.getBooleanOption(Activator.ID + "/keyservice/debug", false); //$NON-NLS-1$
		} else {
			DEBUG_REPOSITORY_CREDENTIALS = false;
			DEBUG_REPOSITORY_TRANSPORT = false;
			DEBUG_KEY_SERVICE = false;
		}
	}

	public static void debug(String name, String message) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("["); //$NON-NLS-1$
		buffer.append(Activator.ID + "-" + name); //$NON-NLS-1$
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] " + LINE_SEPARATOR); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}

	public static void debug(String name, String message, Object... keyValueArray) {
		if (keyValueArray == null || keyValueArray.length == 0) {
			debug(name, message);
		} else {
			Map<Object, Object> params = new LinkedHashMap<>(keyValueArray.length / 2);
			for (int i = 0; i < keyValueArray.length; i += 2) {
				params.put(keyValueArray[i], keyValueArray[i + 1]);
			}
			StringBuilder buffer = new StringBuilder();
			buffer.append(message);
			buffer.append(formatMap(params, true, true));
			debug(name, buffer.toString());
		}

		StringBuilder buffer = new StringBuilder();
		buffer.append("["); //$NON-NLS-1$
		buffer.append(Activator.ID + "-" + name); //$NON-NLS-1$
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] " + LINE_SEPARATOR); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}

	public static String formatArray(Object[] array, boolean toString, boolean newLines) {
		if (array == null || array.length == 0) {
			return "[]"; //$NON-NLS-1$
		}

		StringBuilder buffer = new StringBuilder();
		buffer.append('[');
		int i = 0;
		for (;;) {
			if (toString) {
				buffer.append(array[i].toString());
			} else {
				buffer.append(array[i].getClass().getName());
			}
			i++;
			if (i == array.length) {
				break;
			}
			buffer.append(',');
			if (newLines) {
				buffer.append(DebugHelper.LINE_SEPARATOR);
			} else {
				buffer.append(' ');
			}
		}
		buffer.append(']');
		return buffer.toString();
	}

	public static String formatMap(Map<?, ?> map, boolean toString, boolean newLines) {
		if (map == null || map.size() == 0) {
			return "[]"; //$NON-NLS-1$
		}

		StringBuilder buffer = new StringBuilder();
		buffer.append('[');
		for (Entry<?, ?> e : map.entrySet()) {
			buffer.append(e.getKey());
			buffer.append('=');
			if (toString) {
				buffer.append(e.getValue().toString());
			} else {
				buffer.append(e.getValue().getClass().getName());
			}

			buffer.append(',');
			if (newLines) {
				buffer.append(DebugHelper.LINE_SEPARATOR);
				buffer.append("    "); //$NON-NLS-1$
			} else {
				buffer.append(' ');
			}
		}
		buffer.append(']');
		return buffer.toString();
	}

}
