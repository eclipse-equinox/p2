/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import junit.framework.Test;

public class SharedBundleProductTestSuite extends ReconcilerTestSuite {

	public SharedBundleProductTestSuite() {
		super();
	}

	public SharedBundleProductTestSuite(String propertyToPlatformArchive) {
		super(propertyToPlatformArchive);
	}

	@Override
	public Test getInitializationTest() {
		return new AbstractSharedBundleProductTest("initialize", getPlatformArchive());
	}

	@Override
	public Test getCleanUpTest() {
		return new AbstractSharedBundleProductTest("cleanup");
	}

}
