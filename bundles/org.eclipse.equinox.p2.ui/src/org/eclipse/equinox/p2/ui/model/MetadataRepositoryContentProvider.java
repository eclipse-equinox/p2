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

/**
 * Content provider for metadata repositories. The raw repositories are the
 * elements, and the raw IU's are the children of the repositories.
 * 
 * @since 3.4
 * 
 */
public class MetadataRepositoryContentProvider extends RepositoryContentProvider {
	public Object[] getElements(Object input) {
		if (input == null)
			return getChildren(new AllMetadataRepositories());
		return getChildren(input);
	}
}