package org.eclipse.equinox.internal.p2.director.app;

import java.io.Closeable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.osgi.framework.log.FrameworkLog;

/**
 * Default implementation used for command line director invocations
 */
public class DefaultLog implements ILog, Closeable {
	private boolean failed = false;

	/**
	 * Sends logs to the {@link LogHelper standard p2 log}
	 */
	@Override
	public void log(IStatus status) {
		LogHelper.log(status);

		if (status.getSeverity() == IStatus.ERROR) {
			failed = true;
		}
	}

	/**
	 * Sends messages to {@link System#out}
	 */
	@Override
	public void printOut(String message) {
		System.out.println(message);
	}

	/**
	 * Sends messages to {@link System#err}
	 */
	@Override
	public void printErr(String message) {
		System.err.println(message);
	}

	/**
	 * If failures were detected print a final message with the location of the log file
	 */
	@Override
	public void close() {
		if (!failed) {
			return;
		}

		FrameworkLog fwLog = ServiceHelper.getService(Activator.getContext(), FrameworkLog.class);
		if (fwLog == null) {
			return;
		}

		printErr("There were errors. See log file: " + fwLog.getFile()); //$NON-NLS-1$
	}
}
