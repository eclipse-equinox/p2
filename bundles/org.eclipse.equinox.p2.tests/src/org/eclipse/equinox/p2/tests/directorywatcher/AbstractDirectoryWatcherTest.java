/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	protected Set toRemove = new HashSet();

	/*
	 * Constructor for the class.
	 */
	public AbstractDirectoryWatcherTest(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.tests.AbstractProvisioningTest#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		for (Iterator iter = toRemove.iterator(); iter.hasNext();)
			delete((File) iter.next());
		toRemove = new HashSet();
	}

}
