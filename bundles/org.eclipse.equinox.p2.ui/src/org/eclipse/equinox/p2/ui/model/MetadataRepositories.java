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
package org.eclipse.equinox.p2.ui.model;

import org.eclipse.equinox.p2.ui.query.IQueryProvider;
import org.eclipse.equinox.p2.ui.query.QueriedElement;

/**
 * Element class that represents the root of a metadata
 * repository viewer.  Its children are obtained using the
 * metadata repository query specified by the client in the
 * content provider.
 * 
 * @since 3.4
 *
 */
public class MetadataRepositories extends QueriedElement {

	protected int getQueryType() {
		return IQueryProvider.METADATA_REPOS;
	}

}
