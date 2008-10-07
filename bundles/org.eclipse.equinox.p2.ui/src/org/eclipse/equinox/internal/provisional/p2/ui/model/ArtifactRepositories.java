/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.model;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.internal.provisional.p2.ui.ElementQueryDescriptor;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.QueryProvider;

/**
 * Element class that represents the root of an artifact
 * repository viewer.  Its children are the artifact repositories
 * obtained using the query installed in the content provider.
 * 
 * @since 3.4
 *
 */
public class ArtifactRepositories extends RemoteQueriedElement {

	public ArtifactRepositories() {
		super(null);
	}

	protected int getDefaultQueryType() {
		return QueryProvider.ARTIFACT_REPOS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return ProvUIMessages.Label_Repositories;
	}

	/*
	 * (non-Javadoc)
	 * Overridden because we know that the queryable artifact repo manager can handle a null query
	 * @see org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement#isSufficientForQuery(org.eclipse.equinox.internal.provisional.p2.ui.query.ElementQueryDescriptor)
	 */
	// TODO this is not ideal
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=224504
	protected boolean isSufficientForQuery(ElementQueryDescriptor queryDescriptor) {
		return queryDescriptor.collector != null && queryDescriptor.queryable != null;
	}

}
