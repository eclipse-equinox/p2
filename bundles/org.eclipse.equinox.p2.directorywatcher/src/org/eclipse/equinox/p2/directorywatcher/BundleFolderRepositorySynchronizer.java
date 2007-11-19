package org.eclipse.equinox.p2.directorywatcher;

import java.io.File;
import org.eclipse.equinox.internal.p2.directorywatcher.Activator;

public class BundleFolderRepositorySynchronizer {

	private final DirectoryWatcher watcher;

	public BundleFolderRepositorySynchronizer(File directory) {
		this(new File[] {directory});
	}

	public BundleFolderRepositorySynchronizer(File[] directories) {
		watcher = new DirectoryWatcher(directories);
		RepositoryListener listener = new RepositoryListener(Activator.getContext(), Integer.toString(directories[0].hashCode()));
		watcher.addListener(listener);
	}

	public void start(long pollFrequency) {
		watcher.start(pollFrequency);
	}

	public void stop() {
		watcher.stop();
	}

	public void poll() {
		watcher.poll();

	}
}
