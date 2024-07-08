/*******************************************************************************
 * Copyright (c) 2024 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import org.eclipse.equinox.p2.publisher.eclipse.IMacOsBundleUrlType;

record MacOsBundleUrlType(String scheme, String name) implements IMacOsBundleUrlType {

	@Override
	public String getScheme() {
		return scheme();
	}

	@Override
	public String getName() {
		return name();
	}
}
