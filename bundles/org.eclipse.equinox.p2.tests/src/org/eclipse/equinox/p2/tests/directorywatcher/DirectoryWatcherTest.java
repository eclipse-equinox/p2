/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class DirectoryWatcherTest extends AbstractProvisioningTest {

	public DirectoryWatcherTest(String name) {
		super(name);
	}

	public void testCreateDirectoryWatcherNullDirectory() throws Exception {
		try {
			new DirectoryWatcher((File) null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testCreateDirectoryWatcherOneDirectory() throws Exception {
		URL base = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher1");
		File folder = new File(FileLocator.toFileURL(base).getPath());
		DirectoryWatcher watcher = new DirectoryWatcher(folder);
		watcher.start();
		watcher.stop();
	}

	public void testCreateDirectoryWatcherProps() throws Exception {
		URL base = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher1");
		File folder = new File(FileLocator.toFileURL(base).getPath());

		Hashtable<String, String> props = new Hashtable<>();
		props.put(DirectoryWatcher.DIR, folder.getAbsolutePath());

		DirectoryWatcher watcher = new DirectoryWatcher(props, TestActivator.getContext());
		watcher.start();
		watcher.stop();
	}

	public void testDirectoryWatcherListener() throws Exception {
		URL base = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher1");
		File folder = new File(FileLocator.toFileURL(base).getPath());

		Hashtable<String, String> props = new Hashtable<>();
		props.put(DirectoryWatcher.DIR, folder.getAbsolutePath());

		DirectoryWatcher watcher = new DirectoryWatcher(props, TestActivator.getContext());
		final List<File> list = Collections.synchronizedList(new ArrayList<>());
		DirectoryChangeListener listener = new DirectoryChangeListener() {

			@Override
			public boolean added(File file) {
				if (file.getName().equals("CVS"))
					return false;
				list.add(file);
				return true;
			}

			@Override
			public boolean changed(File file) {
				return false;
			}

			@Override
			public boolean removed(File file) {
				if (file.getName().equals("CVS"))
					return false;
				list.remove(file);
				return true;
			}

			@Override
			public boolean isInterested(File file) {
				return true;
			}

			@Override
			public Long getSeenFile(File file) {
				return null;
			}

		};
		watcher.addListener(listener);
		watcher.poll();
		assertEquals(2, list.size());
	}
}
