/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueryContext;

/**
 * AvailableIUViewQueryContext defines the different ways
 * available IUs can be viewed in the SDK UI.
 */
public class AvailableIUViewQueryContext extends QueryContext {
	public static final int VIEW_BY_CATEGORY = 1;
	public static final int VIEW_BY_REPO = 2;
	public static final int VIEW_FLAT = 3;

	// Default to repo as this provides the fastest information
	private int view = VIEW_BY_REPO;

	public AvailableIUViewQueryContext(int viewType) {
		this.view = viewType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueryContext#getQueryType()
	 */
	public int getQueryType() {
		if (view == VIEW_BY_REPO)
			return IQueryProvider.METADATA_REPOS;
		return IQueryProvider.AVAILABLE_IUS;
	}

	public int getViewType() {
		return view;
	}

	public void setViewType(int viewType) {
		view = viewType;
	}
}
