/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import java.io.File;
import java.io.IOException;

/**
 * LazyBackupStore is a BackupStore that only instantiates a real backup store
 * when needed.
 * 
 *
 */
public class LazyBackupStore implements IBackupStore {
	private BackupStore delegate;
	private final String prefix;

	/**
	 * Creates a new lazy backup store
	 * @param prefix The prefix to use in constructing the backup store directory
	 */
	public LazyBackupStore(String prefix) {
		this.prefix = prefix;
	}

	public boolean backup(File file) throws IOException {
		loadDelegate();
		return delegate.backup(file);
	}

	public boolean backupDirectory(File file) throws IOException {
		loadDelegate();
		return delegate.backupDirectory(file);
	}

	public void discard() {
		if (delegate == null)
			return;
		delegate.discard();
	}

	public void restore() throws IOException {
		if (delegate == null)
			return;
		delegate.restore();
	}

	private void loadDelegate() {
		if (delegate != null)
			return;
		delegate = new BackupStore(null, prefix);
	}

	public String getBackupName() {
		loadDelegate();
		return delegate.getBackupName();
	}

	public boolean backupCopy(File file) throws IOException {
		loadDelegate();
		return delegate.backupCopy(file);
	}

	public void backupCopyAll(File file) throws IOException {
		loadDelegate();
		delegate.backupCopyAll(file);
	}

	public void backupAll(File file) throws IOException {
		loadDelegate();
		delegate.backupAll(file);
	}
}
