/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import org.eclipse.core.runtime.IStatus;

public class TaskHelper {
	public static StringBuffer statusToString(IStatus status, StringBuffer b) {
		return statusToString(status, -1, b);
	}

	public static StringBuffer statusToString(IStatus status, int severities, StringBuffer b) {
		IStatus[] nestedStatus = status.getChildren();
		if (b == null) {
			b = new StringBuffer();
		}
		if (severities == -1 || (status.getSeverity() & severities) != 0) {
			if (b.length() > 0) {
				b.append('\n');
			}
			b.append(status.getMessage());
		}
		for (IStatus s : nestedStatus) {
			statusToString(s, severities, b);
		}
		return b;
	}
}
