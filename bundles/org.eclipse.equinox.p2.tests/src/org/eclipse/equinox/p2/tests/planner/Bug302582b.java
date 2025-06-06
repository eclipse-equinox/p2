/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.planner;

/**
 * Same as Bug302582 except we are trying to install the IUs into
 * a base profile from the Eclipse SDK 3.6 M5. (already includes v1.0 of
 * the HelloWorld bundle)
 */
public class Bug302582b extends Bug302582 {

	@Override
	protected String getProfileId() {
		return "SDKProfile";
	}
}
