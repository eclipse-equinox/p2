package org.eclipse.equinox.p2.tests.directorywatcher;

import java.io.File;
import java.net.URL;
import java.util.*;
import junit.framework.TestCase;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.p2.tests.TestActivator;

public class DirectoryWatcherTest extends TestCase {

	public DirectoryWatcherTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
	}

	public void testCreateDirectoryWatcherZeroDirectory() throws Exception {
		try {
			new DirectoryWatcher(new File[] {});
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testCreateDirectoryWatcherOneDirectory() throws Exception {
		URL base = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher1");
		File folder = new File(FileLocator.toFileURL(base).getPath());
		DirectoryWatcher watcher = new DirectoryWatcher(new File[] {folder});
		watcher.start();
		watcher.stop();
	}

	public void testCreateDirectoryWatcherTwoDirectory() throws Exception {
		URL base1 = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher1");
		File folder1 = new File(FileLocator.toFileURL(base1).getPath());

		URL base2 = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher2");
		File folder2 = new File(FileLocator.toFileURL(base2).getPath());

		DirectoryWatcher watcher = new DirectoryWatcher(new File[] {folder1, folder2});
		watcher.start();
		watcher.stop();
	}

	public void testCreateDirectoryWatcherProps() throws Exception {
		URL base = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher1");
		File folder = new File(FileLocator.toFileURL(base).getPath());

		Hashtable props = new Hashtable();
		props.put(DirectoryWatcher.DIR, folder.getAbsolutePath());

		DirectoryWatcher watcher = new DirectoryWatcher(props, TestActivator.getContext());
		watcher.start();
		watcher.stop();
	}

	public void testDirectoryWatcherListener() throws Exception {
		URL base = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher1");
		File folder = new File(FileLocator.toFileURL(base).getPath());

		Hashtable props = new Hashtable();
		props.put(DirectoryWatcher.DIR, folder.getAbsolutePath());

		DirectoryWatcher watcher = new DirectoryWatcher(props, TestActivator.getContext());
		final List list = Collections.synchronizedList(new ArrayList());
		DirectoryChangeListener listener = new DirectoryChangeListener() {

			public boolean added(File file) {
				if (file.getName().equals("CVS"))
					return false;
				list.add(file);
				return true;
			}

			public boolean changed(File file) {
				return false;
			}

			public boolean removed(File file) {
				if (file.getName().equals("CVS"))
					return false;
				list.remove(file);
				return true;
			}

			public String[] getExtensions() {
				return new String[] {""};
			}

			public Long getSeenFile(File file) {
				return null;
			}

			public void startPoll() {

			}

			public void stopPoll() {
			}

		};
		watcher.addListener(listener);
		watcher.poll();
		assertEquals(2, list.size());
	}
}
