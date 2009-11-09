/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.artifact.repository;

import org.eclipse.equinox.internal.provisional.p2.metadata.query.IMatchQuery;
import org.eclipse.equinox.p2.metadata.query.IQuery;

public interface IArtifactQuery extends IMatchQuery {
	public static final String ACCEPT_KEYS = "AcceptArtifactKeys"; //$NON-NLS-1$
	public static final String ACCEPT_DESCRIPTORS = "AcceptArtifactDescriptors"; //$NON-NLS-1$

	/**
	 * Return whether or not this query is interested in IArtifactKey objects
	 * This method can be accessed using {@link IQuery#getProperty(String)} with parameter
	 * {@link #ACCEPT_KEYS}.
	 * @return Boolean
	 */
	public Boolean getAcceptArtifactKeys();

	/**
	 * Return whether or not this query is interested in IArtifactDescriptor objects
	 * This method can be accessed using {@link IQuery#getProperty(String)} with
	 * parameter {@link #ACCEPT_DESCRIPTORS}.
	 * @return Boolean
	 */
	public Boolean getAcceptArtifactDescriptors();
}
