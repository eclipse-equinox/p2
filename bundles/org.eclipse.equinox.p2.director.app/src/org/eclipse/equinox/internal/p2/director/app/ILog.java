package org.eclipse.equinox.internal.p2.director.app;

import org.eclipse.core.runtime.IStatus;

public interface ILog {

	// Log a status
	public void log(IStatus status);

	public void log(String message);

	// Notify that logging is completed & cleanup resources 
	public void close();
}
