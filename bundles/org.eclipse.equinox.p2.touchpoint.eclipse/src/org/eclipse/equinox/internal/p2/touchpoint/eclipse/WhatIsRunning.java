/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdmin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;

public class WhatIsRunning {
	public BundleInfo[] getBundlesBeingRun() {
		return getFrameworkManipulator().getConfigData().getBundles();
	}

	private Manipulator getFrameworkManipulator() {
		FrameworkAdmin fwAdmin = LazyManipulator.getFrameworkAdmin();
		if (fwAdmin != null)
			return fwAdmin.getRunningManipulator();
		return null;
	}
}
