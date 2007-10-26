/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core.helpers;

import java.util.*;
import org.eclipse.core.runtime.*;

/**
 * An variation of the MultiStatus. Note that this does not
 * extend runtime.MultiStatus, instead it extends Status, because
 * the purpose of this class is to avoid a few shortcomings of
 * runtime.MultiStatus. This is different as follows:
 *      - Add only non-OK status as child (this prevents bloat).
 *      - Children is a list instead of an array; helps when add()
 *        is called more often than getChildren()
 */
public class MultiStatus extends Status {

	public final static String bundleId = "org.eclipse.equinox.p2"; //$NON-NLS-1$
	public static final int STATUS_CODE_SUCCESS = 0;

	// Use ArrayList rather than List so ensureCapacity() is available.
	private ArrayList children = null;

	/**
	 * Uses the default code, STATUS_CODE_SUCCESS and an empty message.
	 * Either set the message later, or add this to another MultiStatus.
	 */
	public MultiStatus() {
		this(STATUS_CODE_SUCCESS, ""); //$NON-NLS-1$
	}

	/**
	 * Uses the default code, STATUS_CODE_SUCCESS.
	 * @param msg The status message.
	 */
	public MultiStatus(String msg) {
		this(STATUS_CODE_SUCCESS, msg);
	}

	/**
	 * A MultiStatus with one child.
	 */
	public MultiStatus(String msg, IStatus child) {
		this(msg);
		add(child);
	}

	/**
	 * This provides a way to use a specific code to
	 * indicate whether this status has been 'touched'
	 * or not. This will differentiate "operation done
	 * and successful" from "operation was not done and
	 * successful" if code < STATUS_CODE_SUCCESS.
	 * @param code The status code.
	 * @param msg The status message.
	 */
	public MultiStatus(int code, String msg) {
		this(code, msg, null);
	}

	/**
	 * A MultiStatus with an exception.
	 */
	public MultiStatus(int code, String msg, Throwable exception) {
		this(code, msg, null, exception);
	}

	/**
	 * A MultiStatus with children.
	 */
	public MultiStatus(int code, String msg, IStatus[] nested, Throwable exception) {
		this(OK, bundleId, code, msg, nested, exception);
	}

	/**
	 * For creation from outside of the default plug-in.
	 */
	public MultiStatus(String pluginId, int code, String msg, Throwable exception) {
		this(OK, pluginId, code, msg, null, exception);
	}

	/**
	 * For creation from outside of the default plugin and with children.
	 */
	public MultiStatus(String pluginId, int code, IStatus[] nested, String msg, Throwable exception) {
		this(OK, pluginId, code, msg, nested, exception);
	}

	/**
	 * A MultiStatus with everything.
	 */
	public MultiStatus(int severity, String pluginId, int code, String msg, IStatus[] nested, Throwable exception) {
		super(severity, pluginId, code, msg, exception);
		if (nested != null && nested.length > 0) {
			addAll(Arrays.asList(nested));
		}
	}

	/**
	 * A new MultiStatus with no children, based on a Status.
	 */
	public MultiStatus(Status status) {
		super(status.getSeverity(), status.getPlugin(), status.getCode(), status.getMessage(), status.getException());
	}

	/**
	 * Does this status indicate an error or cancellation.
	 */
	public boolean isErrorOrCancel() {
		return matches(ERROR | CANCEL);
	}

	public void setCanceled() {
		setSeverity(getSeverity() | CANCEL);
	}

	public void setMessage(String message) {
		super.setMessage(message);
	}

	/**
	 * Adds the given status as a child. Even if child is a
	 * multi-status it is attached as a single child. This
	 * helps in creating a tree instead of a flat list.
	 * This guards against a null child. It will add the child
	 * to the children only if the child has a non-ok severity.
	 * @param child An IStatus object to be added as a child.
	 */
	public void add(IStatus child) {
		if (child == null)
			return;
		setCumulativeCode(child);
		// Add only non-OK children
		if (!child.isOK()) {
			if (child.isMultiStatus() && child.getMessage().length() == 0) {
				addAll(child); // no message at root so just add children
			} else {
				ensureExtraCapacity(1);
				children.add(child);
			}
			setCumulativeSeverity(child);
		}
	}

	/**
	 * Add a collection as children of this MultiStatus, using add().
	 */
	public void addAll(Collection nested) {
		ensureExtraCapacity(nested.size());
		for (Iterator i = nested.iterator(); i.hasNext();) {
			IStatus child = (IStatus) i.next();
			add(child);
		}
	}

	/**
	 * Adds the children of the given status as its own
	 * children. This internally uses add(IStatus). This
	 * guards against a null status.
	 * @param status A MultiStatus IStatus object whose children
	 * are to be added to this children.
	 */
	public void addAll(IStatus status) {
		if (status == null || !status.isMultiStatus()) {
			add(status);
			return;
		}
		IStatus[] nested = status.getChildren();
		ensureExtraCapacity(nested.length);
		for (int i = 0; i < nested.length; i++) {
			add(nested[i]);
		}
	}

	/**
	 * Merges the given status into this MultiStatus.
	 * Equivalent to <code>add(status)</code> if the
	 * given status is not a mMultiStatus. 
	 * Equivalent to <code>addAll(status)</code> if the
	 * given status is a MultiStatus. 
	 *
	 * @param status the status to merge into this one
	 * @see #add(IStatus)
	 * @see #addAll(IStatus)
	 */
	public void merge(IStatus status) {
		Assert.isLegal(status != null);
		if (!status.isMultiStatus()) {
			add(status);
		} else {
			addAll(status);
		}
	}

	/*
	 * @see IStatus#getChildren()
	 */
	public IStatus[] getChildren() {
		IStatus[] result = new IStatus[children == null ? 0 : children.size()];
		if (result.length > 0) {
			children.toArray(result);
		}
		return result;
	}

	/*
	 * @see IStatus#isMultiStatus()
	 */
	public boolean isMultiStatus() {
		return true;
	}

	/**
	 * Collapses the children into a flat list.
	 * If all the children are non-MultiStatus,
	 * this is essentially getChildren().
	 * @return An array of IStatus objects.
	 * @see #getChildren()
	 */
	public IStatus[] getLeaves() {
		List leaves = MultiStatusUtil.getStatusLeaves(this);
		IStatus[] result = new IStatus[leaves.size()];
		leaves.toArray(result);
		return result;
	}

	// Ensure we have space for count more children,
	// allocate or extends as needed.
	private void ensureExtraCapacity(int count) {
		if (this.children == null) {
			this.children = new ArrayList(count < 4 ? 4 : count);
		} else {
			this.children.ensureCapacity(this.children.size() + count);
		}
	}

	private void setCumulativeSeverity(IStatus child) {
		int childSeverity = child.getSeverity();
		if (childSeverity > this.getSeverity()) {
			this.setSeverity(childSeverity);
		}
	}

	private void setCumulativeCode(IStatus child) {
		int childCode = child.getCode();
		if (childCode > this.getCode()) {
			this.setCode(childCode);
		}
	}

	// This implementation of getMessage causes problems: in add(IStatus) we test
	// child.getMessage().length() == 0.  This change prevents that from ever being the case.
	// We want adding a MultiStatus with no message to be equivalent to adding its children.
	//    /**
	//     *  If the message is empty, iterates through the children at the time of
	//     *  invocation of this message and returns the first non-blank message
	//     *  @return Message string
	//     */
	//    public String getMessage() {
	//        String msg = super.getMessage();
	//        if (this.children != null) {
	//            for (Iterator i = this.children.iterator(); msg.trim().length() == 0 && i.hasNext(); ) {
	//                IStatus status = (IStatus) i.next();
	//                msg = status.getMessage();
	//            }
	//        }
	//        return msg;
	//    }
}
