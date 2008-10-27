/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.download;

import junit.framework.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.DownloadManager;

/**
 * Simple tests of {@link DownloadManager} API.
 */
public class DownloadManagerTest extends TestCase {
	public static Test suite() {
		return new TestSuite(DownloadManagerTest.class);
	}

	/**
	 * Tests invocation of DownloadManager when there is nothing to download.
	 */
	public void testEmpty() {
		DownloadManager manager = new DownloadManager(null);
		IStatus result = manager.start(null);
		assertTrue("1.0", result.isOK());
	}

}
