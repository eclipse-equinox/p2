/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
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

import org.eclipse.osgi.util.NLS;

enum FeatureInstallMode {
	INCLUDE("include"), ROOT("root"); //$NON-NLS-1$ //$NON-NLS-2$

	private final String attributeValue;

	private FeatureInstallMode(String attributeValue) {
		this.attributeValue = attributeValue;
	}

	public static FeatureInstallMode parse(String value) {
		if (value == null) {
			return INCLUDE;
		}
		for (FeatureInstallMode mode : FeatureInstallMode.values()) {
			if (mode.attributeValue.equals(value))
				return mode;
		}
		throw new IllegalArgumentException(NLS.bind(Messages.exception_invalidFeatureInstallMode, value));
	}

}
