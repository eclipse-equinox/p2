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

package org.eclipse.equinox.p2.ui.viewers;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.p2.ui.query.IProvElementQueryProvider;

/**
 * Content provider for available software in any number of repos.
 * 
 * @since 3.4
 * 
 */
public class AvailableIUContentProvider extends RepositoryContentProvider {

	public AvailableIUContentProvider(IProvElementQueryProvider queryProvider) {
		super(queryProvider);
	}

	public Object[] getElements(Object input) {
		// Overridden to get the children of each element as the elements.
		Object[] elements = super.getElements(input);
		Set allChildren = new HashSet();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof ProvElement) {
				Object[] children = ((ProvElement) elements[i]).getChildren(elements[i]);
				for (int j = 0; j < children.length; j++)
					allChildren.add(children[j]);
			}
		}
		return allChildren.toArray();
	}

}