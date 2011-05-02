/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.*;

public class MergeResultsAction extends AbstractPublisherAction {

	private IPublisherAction[] actions;
	private int mode;

	public MergeResultsAction(IPublisherAction[] actions, int mode) {
		this.actions = actions;
		this.mode = mode;
	}

	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		MultiStatus finalStatus = new MultiStatus(MergeResultsAction.class.getName(), 0, "publishing result", null); //$NON-NLS-1$
		for (int i = 0; i < actions.length; i++) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			IPublisherAction action = actions[i];
			IPublisherResult result = new PublisherResult();
			finalStatus.merge(action.perform(publisherInfo, result, monitor));
			results.merge(result, mode);
		}
		if (!finalStatus.isOK())
			return finalStatus;
		return Status.OK_STATUS;
	}
}
