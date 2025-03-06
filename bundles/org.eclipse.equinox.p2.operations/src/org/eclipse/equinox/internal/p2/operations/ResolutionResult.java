/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.operations;

import java.util.HashMap;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * ResolutionResult describes problems in a provisioning plan in a structured
 * way that can be presented to a user.
 *
 * @since 2.0
 */
public class ResolutionResult {
	private static final String NESTING_INDENT = "  "; //$NON-NLS-1$

	private final HashMap<IInstallableUnit, MultiStatus> iuToStatusMap = new HashMap<>();
	private MultiStatus summaryStatus;

	public IStatus getSummaryStatus() {
		if (summaryStatus != null) {
			return summaryStatus;
		}
		return Status.OK_STATUS;
	}

	public void setSummaryStatus(MultiStatus status) {
		summaryStatus = status;
	}

	public void addSummaryStatus(IStatus status) {
		if (summaryStatus == null) {
			summaryStatus = new MultiStatus(Constants.BUNDLE_ID, 0, Messages.ResolutionResult_SummaryStatus, null);
		}
		summaryStatus.add(status);
	}

	public IStatus statusOf(IInstallableUnit iu) {
		return iuToStatusMap.get(iu);
	}

	public void addStatus(IInstallableUnit iu, IStatus status) {
		MultiStatus iuSummaryStatus = iuToStatusMap.get(iu);
		if (iuSummaryStatus == null) {
			iuSummaryStatus = new MultiStatus(Constants.BUNDLE_ID, IStatusCodes.IU_REQUEST_ALTERED, new IStatus[] {status}, getIUString(iu), null);
		} else {
			iuSummaryStatus.add(status);
		}
	}

	private String getIUString(IInstallableUnit iu) {
		if (iu == null) {
			return Messages.PlanAnalyzer_Items;
		}
		// Get the iu name in the default locale
		String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
		if (name != null) {
			return name;
		}
		return iu.getId();
	}

	public String getSummaryReport() {
		if (summaryStatus != null) {
			StringBuilder buffer = new StringBuilder();
			appendDetailText(summaryStatus, buffer, -1, false);
			return buffer.toString();
		}
		return ""; //$NON-NLS-1$
	}

	// Answers null if there is nothing to say about the ius
	public String getDetailedReport(IInstallableUnit[] ius) {
		StringBuilder buffer = new StringBuilder();
		for (IInstallableUnit iu : ius) {
			MultiStatus iuStatus = iuToStatusMap.get(iu);
			if (iuStatus != null) {
				appendDetailText(iuStatus, buffer, 0, true);
			}
		}
		String report = buffer.toString();
		if (report.length() == 0) {
			return null;
		}
		return report;
	}

	void appendDetailText(IStatus status, StringBuilder buffer, int indent, boolean includeTopLevelMessage) {
		if (includeTopLevelMessage) {
			for (int i = 0; i < indent; i++) {
				buffer.append(NESTING_INDENT);
			}
			if (status.getMessage() != null) {
				buffer.append(status.getMessage());
			}
		}
		Throwable t = status.getException();
		if (t != null) {
			// A provision (or core) exception occurred.  Get its status message or if none, its top level message.
			// Indent by one more level (note the <=)
			buffer.append('\n');
			for (int i = 0; i <= indent; i++) {
				buffer.append(NESTING_INDENT);
			}
			if (t instanceof CoreException) {
				IStatus exceptionStatus = ((CoreException) t).getStatus();
				if (exceptionStatus != null && exceptionStatus.getMessage() != null) {
					buffer.append(exceptionStatus.getMessage());
				} else {
					String details = t.getLocalizedMessage();
					if (details != null) {
						buffer.append(details);
					}
				}
			} else {
				String details = t.getLocalizedMessage();
				if (details != null) {
					buffer.append(details);
				}
			}
		}
		// Now print the children status info (if there are children)
		IStatus[] children = status.getChildren();
		for (IStatus child : children) {
			if (buffer.length() > 0) {
				buffer.append('\n');
			}
			appendDetailText(child, buffer, indent + 1, true);
		}
	}
}
