/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;

public class MergeResultsAction extends Generator implements IPublishingAction {

	private IPublishingAction[] actions;
	private int mode;

	public MergeResultsAction(IPublishingAction[] actions, int mode) {
		super(null);
		this.actions = actions;
		this.mode = mode;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		for (int i = 0; i < actions.length; i++) {
			IPublishingAction action = actions[i];
			IPublisherResult result = new PublisherResult();
			action.perform(info, result);
			results.merge(result, mode);
		}
		return Status.OK_STATUS;
	}
}
