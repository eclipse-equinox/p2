/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.directorywatcher;

import java.io.File;

public interface IDirectoryChangeListener {

	public void startPoll();

	public void stopPoll();

	public String[] getExtensions();

	public boolean added(File file);

	public boolean removed(File file);

	public boolean changed(File file);

	public Long getSeenFile(File file);
}
