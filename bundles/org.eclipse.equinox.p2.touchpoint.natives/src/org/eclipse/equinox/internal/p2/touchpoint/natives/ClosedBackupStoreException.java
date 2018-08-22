/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

public class ClosedBackupStoreException extends IllegalStateException {
	private static final long serialVersionUID = -5030940685029643678L;

	public ClosedBackupStoreException() {
		super();
	}

	public ClosedBackupStoreException(String message) {
		super(message);
	}

}
