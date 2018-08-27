/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.directorywatcher;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * @since 1.0
 */
public abstract class AbstractDirectoryWatcherTest extends AbstractProvisioningTest {

	// list of File objects to remove later during teardown
	protected Set<File> toRemove = new HashSet<>();

	/*
	 * Constructor for the class.
	 */
	public AbstractDirectoryWatcherTest(String name) {
		super(name);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		for (Iterator<File> iter = toRemove.iterator(); iter.hasNext();)
			delete(iter.next());
		toRemove = new HashSet<>();
	}

}
