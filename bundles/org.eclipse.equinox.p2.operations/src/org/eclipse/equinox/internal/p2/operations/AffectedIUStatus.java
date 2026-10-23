/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.operations;

import org.eclipse.core.runtime.Status;

public class AffectedIUStatus extends Status {

	private final String iuName;

	public AffectedIUStatus(int severity, int code, String message, String iuName) {
		super(severity, Constants.BUNDLE_ID, code, message, null);
		this.iuName = iuName;
	}

	public String getIUName() {
		return iuName;
	}
}
