/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
		if (b == null)
			b = new StringBuffer();
		if (severities == -1 || (status.getSeverity() & severities) != 0) {
			if (b.length() > 0)
				b.append('\n');
			b.append(status.getMessage());
		}
		for (int i = 0; i < nestedStatus.length; i++) {
			statusToString(nestedStatus[i], severities, b);
		}
		return b;
	}
}
