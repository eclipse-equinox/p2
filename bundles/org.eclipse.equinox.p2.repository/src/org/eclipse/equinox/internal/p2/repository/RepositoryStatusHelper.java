/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc, and other.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the individual
 * copyright holders listed above, as Initial Contributors under such license.
 * The text of such license is available at www.eclipse.org.
 * Contributors:
 * 	Cloudsmith Inc. - Initial API and implementation
 *  IBM Corporation - Original Implementation of checkPermissionDenied
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;

/**
 * RepositoryStatusHelper is a utility class for processing of exceptions and status.
 */
public abstract class RepositoryStatusHelper {

	private static final long serialVersionUID = 1L;
	protected static final String SERVER_REDIRECT = "Server redirected too many times"; //$NON-NLS-1$

	public static IStatus createStatus(String nlsMessage, Object arg) {
		return createExceptionStatus(null, nlsMessage, new Object[] {arg});
	}

	public static IStatus createStatus(String nlsMessage, Object arg1, Object arg2) {
		return createExceptionStatus(null, nlsMessage, new Object[] {arg1, arg2});
	}

	public static IStatus createStatus(String nlsMessage, Object arg1, Object arg2, Object arg3) {
		return createExceptionStatus(null, nlsMessage, new Object[] {arg1, arg2, arg3});
	}

	public static IStatus createStatus(String nlsMessage, Object[] args) {
		return createExceptionStatus(null, nlsMessage, args);
	}

	public static IStatus createStatus(String nlsMessage) {
		return createExceptionStatus(null, nlsMessage, new Object[] {});
	}

	public static IStatus createExceptionStatus(Throwable cause) {
		return (cause instanceof CoreException) ? ((CoreException) cause).getStatus() : new Status(IStatus.ERROR, Activator.ID, IStatus.OK, cause.getMessage(), cause);
	}

	public static IStatus createExceptionStatus(Throwable cause, String nlsMessage, Object[] args) {
		if (args != null && args.length > 0)
			nlsMessage = NLS.bind(nlsMessage, args);
		return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, nlsMessage, cause);
	}

	public static IStatus createExceptionStatus(Throwable cause, String nlsMessage, Object arg1, Object arg2, Object arg3) {
		return createExceptionStatus(cause, nlsMessage, new Object[] {arg1, arg2, arg3});
	}

	public static IStatus createExceptionStatus(Throwable cause, String nlsMessage, Object arg1, Object arg2) {
		return createExceptionStatus(cause, nlsMessage, new Object[] {arg1, arg2});
	}

	public static IStatus createExceptionStatus(Throwable cause, String nlsMessage, Object arg1) {
		return createExceptionStatus(cause, nlsMessage, new Object[] {arg1});
	}

	public static IStatus createExceptionStatus(Throwable cause, String nlsMessage) {
		return createExceptionStatus(cause, nlsMessage, new Object[] {});
	}

	public static void deeplyPrint(Throwable e, PrintStream strm, boolean stackTrace) {
		deeplyPrint(e, strm, stackTrace, 0);
	}

	public static CoreException fromMessage(String nlsMessage, Object[] args) {
		return fromExceptionMessage(null, nlsMessage, args);
	}

	public static CoreException fromMessage(String nlsMessage, Object arg1) {
		return fromExceptionMessage(null, nlsMessage, new Object[] {arg1});
	}

	public static CoreException fromMessage(String nlsMessage, Object arg1, Object arg2) {
		return fromExceptionMessage(null, nlsMessage, new Object[] {arg1, arg2});
	}

	public static CoreException fromMessage(String nlsMessage, Object arg1, Object arg2, Object arg3) {
		return fromExceptionMessage(null, nlsMessage, new Object[] {arg1, arg2, arg3});
	}

	public static CoreException fromMessage(String nlsMessage) {
		return fromExceptionMessage(null, nlsMessage, new Object[] {});
	}

	public static CoreException fromExceptionMessage(Throwable cause, String nlsMessage, Object[] args) {
		CoreException ce = new CoreException(createExceptionStatus(cause, nlsMessage, args));
		if (cause != null)
			ce.initCause(cause);
		return ce;
	}

	public static CoreException fromExceptionMessage(Throwable cause, String nlsMessage, Object arg1, Object arg2, Object arg3) {
		return fromExceptionMessage(cause, nlsMessage, new Object[] {arg1, arg2, arg3});
	}

	public static CoreException fromExceptionMessage(Throwable cause, String nlsMessage, Object arg1, Object arg2) {
		return fromExceptionMessage(cause, nlsMessage, new Object[] {arg1, arg2});
	}

	public static CoreException fromExceptionMessage(Throwable cause, String nlsMessage, Object arg1) {
		return fromExceptionMessage(cause, nlsMessage, new Object[] {arg1});
	}

	public static CoreException fromExceptionMessage(Throwable cause, String nlsMessage) {
		return fromExceptionMessage(cause, nlsMessage, new Object[] {});
	}

	public static Throwable unwind(Throwable t) {
		for (;;) {
			Class tc = t.getClass();

			// We don't use instanceof operator since we want
			// the explicit class, not subclasses.
			//
			if (tc != RuntimeException.class && tc != InvocationTargetException.class && tc != IOException.class)
				break;

			Throwable cause = t.getCause();
			if (cause == null)
				break;

			String msg = t.getMessage();
			if (msg != null && !msg.equals(cause.toString()))
				break;

			t = cause;
		}
		return t;
	}

	public static CoreException unwindCoreException(CoreException exception) {
		IStatus status = exception.getStatus();
		while (status != null && status.getException() instanceof CoreException) {
			exception = (CoreException) status.getException();
			status = exception.getStatus();
		}
		return exception;
	}

	public static CoreException wrap(IStatus status) {
		CoreException e = new CoreException(status);
		Throwable t = status.getException();
		if (t != null)
			e.initCause(t);
		return e;
	}

	public static CoreException wrap(Throwable t) {
		t = unwind(t);
		if (t instanceof CoreException)
			return unwindCoreException((CoreException) t);

		if (t instanceof OperationCanceledException || t instanceof InterruptedException)
			return new CoreException(Status.CANCEL_STATUS);

		String msg = t.toString();
		return fromExceptionMessage(t, msg);
	}

	private static void appendLevelString(PrintStream strm, int level) {
		if (level > 0) {
			strm.print("[0"); //$NON-NLS-1$
			for (int idx = 1; idx < level; ++idx) {
				strm.print('.');
				strm.print(level);
			}
			strm.print(']');
		}
	}

	private static void deeplyPrint(CoreException ce, PrintStream strm, boolean stackTrace, int level) {
		appendLevelString(strm, level);
		if (stackTrace)
			ce.printStackTrace(strm);
		deeplyPrint(ce.getStatus(), strm, stackTrace, level);
	}

	private static void deeplyPrint(IStatus status, PrintStream strm, boolean stackTrace, int level) {
		appendLevelString(strm, level);
		String msg = status.getMessage();
		strm.println(msg);
		Throwable cause = status.getException();
		if (cause != null) {
			strm.print("Caused by: "); //$NON-NLS-1$
			if (stackTrace || !(msg.equals(cause.getMessage()) || msg.equals(cause.toString())))
				deeplyPrint(cause, strm, stackTrace, level);
		}

		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++)
				deeplyPrint(children[i], strm, stackTrace, level + 1);
		}
	}

	private static void deeplyPrint(Throwable t, PrintStream strm, boolean stackTrace, int level) {
		if (t instanceof CoreException)
			deeplyPrint((CoreException) t, strm, stackTrace, level);
		else {
			appendLevelString(strm, level);
			if (stackTrace)
				t.printStackTrace(strm);
			else {
				strm.println(t.toString());
				Throwable cause = t.getCause();
				if (cause != null) {
					strm.print("Caused by: "); //$NON-NLS-1$
					deeplyPrint(cause, strm, stackTrace, level);
				}
			}
		}
	}

	/**
	 * Check if the given exception represents a permission failure (401 for HTTP),
	 * and throw a AuthenticationFailedException if a permission failure was encountered.
	 */
	public static void checkPermissionDenied(Throwable t) throws AuthenticationFailedException {
		if (t instanceof IncomingFileTransferException) {
			if (((IncomingFileTransferException) t).getErrorCode() == 401)
				throw new AuthenticationFailedException();
			IStatus status = ((IncomingFileTransferException) t).getStatus();
			t = status.getException();
		}
		if (t.getClass().getName().equals("javax.security.auth.login.LoginException")) //$NON-NLS-1$
			throw new AuthenticationFailedException();

		if (t == null || !(t instanceof IOException))
			return;

		// try to figure out if we have a 401 by parsing the exception message
		// There is unfortunately no specific exception for "redirected too many times" - which is commonly
		// caused by a failed login.
		//
		String m = t.getMessage();
		if (m != null && (m.indexOf(" 401 ") != -1 || m.indexOf(SERVER_REDIRECT) != -1)) //$NON-NLS-1$
			throw new AuthenticationFailedException();

	}

	/**
	 * Get default "InternalError" ProvisionException.
	 * @param t
	 * @return a default "InternalError"
	 */
	public static ProvisionException internalError(Throwable t) {
		return new ProvisionException(new Status(IStatus.ERROR, Activator.ID, //
				ProvisionException.INTERNAL_ERROR, Messages.repoMan_internalError, t));
	}

	public static IStatus malformedAddressStatus(String address, Throwable t) {
		return new Status(IStatus.ERROR, Activator.ID, //
				ProvisionException.REPOSITORY_INVALID_LOCATION, NLS.bind(Messages.exception_malformedRepoURI, address), t);

	}
}
