/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
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

import java.util.Arrays;

import org.eclipse.core.runtime.IStatus;
import org.osgi.service.log.LogService;

public class LogHelper {

	public static int getLogCode(IStatus status) {
		switch (status.getCode()) {
			case IStatus.CANCEL :
				return LogService.LOG_INFO;
			case IStatus.ERROR :
				return LogService.LOG_ERROR;
			case IStatus.INFO :
				return LogService.LOG_INFO;
			case IStatus.OK :
				return LogService.LOG_INFO;
			case IStatus.WARNING :
				return LogService.LOG_WARNING;
			default :
				return IStatus.INFO;
		}
	}

	/**
	 * @param status
	 * @return String the string version of the status
	 */
	public static String getLogMessage(IStatus status) {
		if (status == null)
			return ""; //$NON-NLS-1$
		StringBuilder buf = new StringBuilder(status.getClass().getName() + '[');
		buf.append("plugin=").append(status.getPlugin()); //$NON-NLS-1$
		buf.append(";code=").append(status.getCode()); //$NON-NLS-1$
		buf.append(";message=").append(status.getMessage()); //$NON-NLS-1$
		buf.append(";severity").append(status.getSeverity()); //$NON-NLS-1$
		buf.append(";exception=").append(status.getException()); //$NON-NLS-1$
		buf.append(";children=").append(Arrays.asList(status.getChildren())) //$NON-NLS-1$
				.append(']');
		return buf.toString();
	}

}
