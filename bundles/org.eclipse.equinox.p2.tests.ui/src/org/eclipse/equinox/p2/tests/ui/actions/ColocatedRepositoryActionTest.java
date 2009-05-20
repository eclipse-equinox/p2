/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.actions;

import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;

/**
 * Abstract class to set up the colocated UI test repo
 */
public abstract class ColocatedRepositoryActionTest extends ActionTest {

	protected Object[] getValidRepoSelection() {
		return new MetadataRepositoryElement[] {new MetadataRepositoryElement(null, testRepoLocation, true)};
	}
}
