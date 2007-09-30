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
import org.eclipse.core.runtime.MultiStatus;

public class MultiStatusUtil {

	private MultiStatusUtil() {
		// No instances, please.
	}

	/**
	 * Does the given status indicate an error or cancellation.
	 */
	public static boolean isErrorOrCancel(IStatus status) {
		return status.matches(IStatus.ERROR | IStatus.CANCEL);
	}

	public static List getStatusLeaves(IStatus root) {
		ArrayList leaves = new ArrayList();
		collectStatusLeaves(root, leaves);
		leaves.trimToSize();
		return leaves;
	}

	public static String getFailureMessage(IStatus status) {
		final String msgSeparator = "; "; //$NON-NLS-1$
		return getFailureMessage(status, msgSeparator);
	}

	public static String getFailureMessage(IStatus status, String msgSeparator) {
		if (status.isMultiStatus()) {
			StringBuffer sb = new StringBuffer(status.getMessage());
			List failures = MultiStatusUtil.getStatusLeaves(status);
			boolean hasNext = false;
			if (!failures.isEmpty()) {
				sb.append(msgSeparator);
				hasNext = true;
			}
			for (Iterator it = failures.iterator(); hasNext;) {
				IStatus failure = (IStatus) it.next();
				sb.append(failure.getMessage());
				hasNext = it.hasNext();
				int lastIndex = sb.length() - 1;
				if (hasNext) {
					if (sb.charAt(lastIndex) == '.') {
						sb.deleteCharAt(lastIndex);
					}
					sb.append(msgSeparator);
				} else {
					if (sb.charAt(lastIndex) != '.') {
						sb.append('.');
					}
				}
			}
			return sb.toString();
		} else
			return status.getMessage();
	}

	public static List getStatusNodes(IStatus root) {
		ArrayList nodes = new ArrayList();
		collectStatusNodes(root, nodes);
		nodes.trimToSize();
		return nodes;
	}

	private static void collectStatusLeaves(IStatus root, List leaves) {
		if (root.isMultiStatus()) {
			IStatus[] children = root.getChildren();
			if (children.length == 0) {
				leaves.add(root);
			} else {
				for (int i = 0; i < children.length; i++) {
					collectStatusLeaves(children[i], leaves);
				}
			}
		} else {
			leaves.add(root);
		}
	}

	private static IStatus newNonMultiStatus(IStatus status) {
		if (status.isMultiStatus())
			return new Status(status.getSeverity(), status.getPlugin(), status.getCode(), status.getMessage(), status.getException());
		else
			return status;
	}

	private static void collectStatusNodes(IStatus root, List nodes) {
		nodes.add(newNonMultiStatus(root));
		if (root.isMultiStatus()) {
			IStatus[] children = root.getChildren();
			for (int i = 0; i < children.length; i++) {
				collectStatusNodes(children[i], nodes);
			}
		}
	}

	public static IStatus newFlattenedStatus(String pluginId, IStatus root, String msg) {
		if (!root.isMultiStatus())
			return root;
		List leaves = MultiStatusUtil.getStatusLeaves(root);
		if (leaves.isEmpty())
			return root;
		IStatus[] children = (IStatus[]) leaves.toArray(new IStatus[leaves.size()]);
		return new MultiStatus(pluginId, root.getCode(), children, msg, null);
	}

	private static boolean hasFilteredStatus(IStatus status, IStatusFilter filter) {
		if (filter.include(status))
			return true;
		if (!filter.considerChildren())
			return false;
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++) {
			IStatus child = children[i];
			if (hasFilteredStatus(child, filter))
				return true;
		}
		return false;
	}

	public interface IStatusFilter {
		boolean include(IStatus status);

		boolean considerChildren();
	}

	public static IStatus newFilteredStatus(String message, IStatus status, IStatusFilter filter) {
		if (!status.isMultiStatus())
			return status;
		LinkedList list = new LinkedList();
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++) {
			IStatus child = children[i];
			if (hasFilteredStatus(child, filter)) {
				list.add(child);
			}
		}
		IStatus[] newChildren = (IStatus[]) list.toArray(new IStatus[list.size()]);

		if (newChildren.length == 0)
			return status;
		return new MultiStatus(status.getPlugin(), status.getCode(), newChildren, message, status.getException());
	}

	public interface IStatusRecoder {
		boolean needsRecoding(IStatus status);

		/**
		 * Recodes the status.
		 * <p>
		 * The returned status will always be a copy even if
		 * no recoding was necessary. If the input is a multi
		 * status the returned status is also a MultiStatus,
		 * but it does not yet have any children.
		 * 
		 * @param status
		 * @return the recoded status
		 */
		IStatus recode(IStatus status);

		IStatus recode(IStatus multiStatus, IStatus[] children);
	}

	private static boolean needsRecoding(IStatus status, IStatusRecoder recoder) {
		if (!status.isMultiStatus())
			return recoder.needsRecoding(status);
		else {
			if (recoder.needsRecoding(status))
				return true;
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++) {
				IStatus child = children[i];
				if (needsRecoding(child, recoder))
					return true;
			}
			return false;
		}
	}

	private static IStatus doSingleRecode(IStatus status, IStatusRecoder recoder) {
		if (recoder.needsRecoding(status))
			return recoder.recode(status);
		return status;
	}

	private static IStatus doSingleRecode(IStatus status, IStatus[] recodedChildren, IStatusRecoder recoder) {
		if (recoder.needsRecoding(status))
			return recoder.recode(status, recodedChildren);
		return new org.eclipse.core.runtime.MultiStatus(status.getPlugin(), status.getCode(), recodedChildren, status.getMessage(), status.getException());
	}

	private static IStatus doRecodeStatus(IStatus status, IStatusRecoder recoder) {
		if (!status.isMultiStatus())
			return doSingleRecode(status, recoder);
		IStatus[] children = status.getChildren();
		IStatus[] recodedChildren = new IStatus[children.length];
		for (int i = 0; i < children.length; i++) {
			IStatus child = children[i];
			IStatus childRecoded = doRecodeStatus(child, recoder);
			recodedChildren[i] = childRecoded;
		}
		IStatus recoded = doSingleRecode(status, recodedChildren, recoder);
		return recoded;
	}

	public static IStatus recodeStatus(IStatus status, IStatusRecoder recoder) {
		if (!needsRecoding(status, recoder))
			return status;
		return doRecodeStatus(status, recoder);
	}

	public static IStatus recodeLevel(IStatus status, final int severityMask, final int newSeverity) {
		return recodeStatus(status, new IStatusRecoder() {

			public boolean needsRecoding(IStatus s) {
				return !s.isMultiStatus() && s.matches(severityMask);
			}

			public IStatus recode(IStatus s) {
				return new Status(newSeverity, s.getPlugin(), s.getCode(), s.getMessage(), s.getException());
			}

			public IStatus recode(IStatus multiStatus, IStatus[] children) {
				throw new AssertionError("should never be called"); //$NON-NLS-1$
			}

		});
	}

}
