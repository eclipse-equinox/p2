/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial implementation and ideas 
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;

/*
 * Abstract class which contains stub methods. Sub-classes to over-ride
 * methods which they are interested in.
 */
public abstract class DirectoryChangeListener {

	public void startPoll() {
		// do nothing
	}

	public void stopPoll() {
		// do nothing
	}

	public boolean isInterested(File file) {
		return false;
	}

	public boolean added(File file) {
		return false;
	}

	public boolean removed(File file) {
		return false;
	}

	public boolean changed(File file) {
		return false;
	}

	//TODO this method name needs to be more descriptive.  getLastModified?
	public Long getSeenFile(File file) {
		return null;
	}
}
