/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public boolean backup(File file) throws IOException {
		loadDelegate();
		return delegate.backup(file);
	}

	@Override
	public boolean backupDirectory(File file) throws IOException {
		loadDelegate();
		return delegate.backupDirectory(file);
	}

	@Override
	public void discard() {
		if (delegate == null)
			return;
		delegate.discard();
	}

	@Override
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

	@Override
	public String getBackupName() {
		loadDelegate();
		return delegate.getBackupName();
	}

	@Override
	public boolean backupCopy(File file) throws IOException {
		loadDelegate();
		return delegate.backupCopy(file);
	}

	@Override
	public void backupCopyAll(File file) throws IOException {
		loadDelegate();
		delegate.backupCopyAll(file);
	}

	@Override
	public void backupAll(File file) throws IOException {
		loadDelegate();
		delegate.backupAll(file);
	}
}
