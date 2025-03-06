/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.*;

public class MergeResultsAction extends AbstractPublisherAction {

	private final IPublisherAction[] actions;
	private final int mode;

	public MergeResultsAction(IPublisherAction[] actions, int mode) {
		this.actions = actions;
		this.mode = mode;
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		MultiStatus finalStatus = new MultiStatus(MergeResultsAction.class.getName(), 0, "publishing result", null); //$NON-NLS-1$
		for (IPublisherAction action : actions) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			IPublisherResult result = new PublisherResult();
			finalStatus.merge(action.perform(publisherInfo, result, monitor));
			results.merge(result, mode);
		}
		if (!finalStatus.isOK()) {
			return finalStatus;
		}
		return Status.OK_STATUS;
	}
}
