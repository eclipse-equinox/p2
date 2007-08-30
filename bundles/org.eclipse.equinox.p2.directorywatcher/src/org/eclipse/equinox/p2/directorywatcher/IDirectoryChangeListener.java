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
