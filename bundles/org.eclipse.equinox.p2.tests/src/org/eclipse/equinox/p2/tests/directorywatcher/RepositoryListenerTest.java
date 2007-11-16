package org.eclipse.equinox.p2.tests.directorywatcher;

import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import junit.framework.TestCase;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.p2.tests.TestActivator;

public class RepositoryListenerTest extends TestCase {

	public RepositoryListenerTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
	}

	public static void copyFile(File source, File target) throws Exception {
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new BufferedInputStream(new FileInputStream(source));
			output = new BufferedOutputStream(new FileOutputStream(target));

			byte[] buffer = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = input.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close input stream on: " + source.getAbsolutePath());
					e.printStackTrace();
				}
			}
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close output stream on: " + target.getAbsolutePath());
					e.printStackTrace();
				}
			}
		}
	}

	public static boolean copyDirectory(File source, File target) throws Exception {
		if (source.exists() && source.isDirectory() && target.exists() && target.isDirectory()) {
			File[] files = source.listFiles();
			for (int i = 0; i < files.length; i++) {
				File newFile = new File(target, files[i].getName());
				if (files[i].isDirectory()) {
					newFile.mkdir();
					copyDirectory(files[i], newFile);
				} else {
					copyFile(files[i], newFile);
				}
			}
		}
		return true;
	}

	public static boolean deleteDirectory(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return directory.delete();
	}

	private void removeContents(File source, File target) throws Exception {
		if (source.exists() && source.isDirectory() && target.exists() && target.isDirectory()) {
			File[] files = source.listFiles();
			for (int i = 0; i < files.length; i++) {
				File doomedFile = new File(target, files[i].getName());
				if (files[i].isDirectory()) {
					deleteDirectory(doomedFile);
				} else {
					doomedFile.delete();
				}
			}
		}
	}

	public void testDirectoryWatcherListener() throws Exception {
		URL base = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher1");
		File baseFolder = new File(FileLocator.toFileURL(base).getPath());

		URL base2 = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher2");
		File baseFolder2 = new File(FileLocator.toFileURL(base2).getPath());

		String tempDir = System.getProperty("java.io.tmpdir");
		File folder = new File(tempDir, "testWatcher");
		deleteDirectory(folder);
		folder.mkdir();

		Hashtable props = new Hashtable();
		props.put(DirectoryWatcher.DIR, folder.getAbsolutePath());
		props.put(DirectoryWatcher.POLL, "500");

		RepositoryListener listener = new RepositoryListener(TestActivator.getContext(), folder);

		assertEquals("1.0", 0, listener.getMetadataRepository().getInstallableUnits(null).length);
		assertEquals("1.1", 0, listener.getArtifactRepository().getArtifactKeys().length);

		DirectoryWatcher watcher = new DirectoryWatcher(props, TestActivator.getContext());
		watcher.addListener(listener);
		watcher.start();

		assertEquals("2.0", 0, listener.getMetadataRepository().getInstallableUnits(null).length);
		assertEquals("2.1", 0, listener.getArtifactRepository().getArtifactKeys().length);

		copyDirectory(baseFolder, folder);
		watcher.poll();
		watcher.close();

		assertEquals("3.0", 1, listener.getMetadataRepository().getInstallableUnits(null).length);
		assertEquals("3.1", 1, listener.getArtifactRepository().getArtifactKeys().length);

		watcher = new DirectoryWatcher(props, TestActivator.getContext());
		watcher.addListener(listener);
		watcher.start();

		assertEquals("4.0", 1, listener.getMetadataRepository().getInstallableUnits(null).length);
		assertEquals("4.1", 1, listener.getArtifactRepository().getArtifactKeys().length);

		copyDirectory(baseFolder2, folder);
		watcher.poll();

		assertEquals("5.0", 2, listener.getMetadataRepository().getInstallableUnits(null).length);
		assertEquals("5.1", 2, listener.getArtifactRepository().getArtifactKeys().length);

		watcher.close();

		watcher = new DirectoryWatcher(props, TestActivator.getContext());
		watcher.addListener(listener);
		watcher.start();

		removeContents(baseFolder, folder);
		watcher.poll();

		assertEquals("6.0", 1, listener.getMetadataRepository().getInstallableUnits(null).length);
		assertEquals("6.1", 1, listener.getArtifactRepository().getArtifactKeys().length);

		watcher.close();

		deleteDirectory(folder);
	}
}
