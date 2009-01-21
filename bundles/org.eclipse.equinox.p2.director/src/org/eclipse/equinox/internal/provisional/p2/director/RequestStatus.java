/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.director;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

public class RequestStatus {
	public static final byte ADDED = 0;
	public static final byte REMOVED = 1;

	private byte initialRequestType;
	private IInstallableUnit iu;
	private int severity;

	public RequestStatus(IInstallableUnit iu, byte initialRequesType, int severity) {
		this.iu = iu;
		this.severity = severity;
		this.initialRequestType = initialRequesType;
	}

	public byte getInitialRequestType() {
		return initialRequestType;
	}

	public IInstallableUnit getIu() {
		return iu;
	}

	public int getSeverity() {
		return severity;
	}
}
