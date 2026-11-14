/****************************************************************************
 * Copyright (c) 2007, 2018 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 *
 */
public class SystemLogService implements LogService {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("Z yyyy.MM.dd HH:mm:ss:S"); //$NON-NLS-1$

	private final String pluginName;

	public SystemLogService(String pluginName) {
		this.pluginName = pluginName;
	}

	private static final String getLogCode(int level) {
		switch (level) {
		case LogService.LOG_INFO:
			return "INFO"; //$NON-NLS-1$
		case LogService.LOG_ERROR:
			return "ERROR"; //$NON-NLS-1$
		case LogService.LOG_DEBUG:
			return "DEBUG"; //$NON-NLS-1$
		case LogService.LOG_WARNING:
			return "WARNING"; //$NON-NLS-1$
		default:
			return "UNKNOWN"; //$NON-NLS-1$
		}
	}

	private final void doLog(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message,
			Throwable t) {
		final StringBuffer buf = new StringBuffer("[log;"); //$NON-NLS-1$
		buf.append(dateFormat.format(new Date())).append(";"); //$NON-NLS-1$
		buf.append(getLogCode(level)).append(";"); //$NON-NLS-1$
		if (sr != null)
			buf.append(sr.getBundle().getSymbolicName()).append(";"); //$NON-NLS-1$
		else
			buf.append(pluginName).append(";"); //$NON-NLS-1$
		buf.append(message).append("]"); //$NON-NLS-1$
		if (t != null) {
			System.err.println(buf.toString());
			t.printStackTrace(System.err);
		} else
			System.out.println(buf.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.log.LogService#log(int, java.lang.String)
	 */
	public void log(int level, String message) {
		log(null, level, message, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.log.LogService#log(int, java.lang.String,
	 * java.lang.Throwable)
	 */
	public void log(int level, String message, Throwable exception) {
		doLog(null, level, message, exception);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.log.LogService#log(org.osgi.framework.ServiceReference,
	 * int, java.lang.String)
	 */
	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.log.LogService#log(org.osgi.framework.ServiceReference,
	 * int, java.lang.String, java.lang.Throwable)
	 */
	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message, Throwable exception) {
		doLog(sr, level, message, exception);
	}

	public org.osgi.service.log.Logger getLogger(String name) {
		throw new UnsupportedOperationException();
	}

	public org.osgi.service.log.Logger getLogger(Class<?> clazz) {
		throw new UnsupportedOperationException();
	}

	public <L extends org.osgi.service.log.Logger> L getLogger(String name, Class<L> loggerType) {
		throw new UnsupportedOperationException();
	}

	public <L extends org.osgi.service.log.Logger> L getLogger(Class<?> clazz, Class<L> loggerType) {
		throw new UnsupportedOperationException();
	}

	public <L extends org.osgi.service.log.Logger> L getLogger(Bundle bundle, String name, Class<L> loggerType) {
		throw new UnsupportedOperationException();
	}
}
