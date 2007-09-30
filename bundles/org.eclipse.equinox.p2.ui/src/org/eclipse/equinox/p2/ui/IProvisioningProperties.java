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
package org.eclipse.equinox.p2.ui;

/**
 * Temporary class
 * 
 * @since 3.4
 */
// TODO this should all be defined in the core
// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=197052
// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=197701
public interface IProvisioningProperties {

	public final static String REPO_ADDED = "org.eclipse.equinox.p2.ui.property.repoadded"; //$NON-NLS-1$
	public final static String REPO_NAME = "org.eclipse.equinox.p2.ui.property.reponame"; //$NON-NLS-1$
	public final static String REPO_REMOVED = "org.eclipse.equinox.p2.ui.property.reporemoved"; //$NON-NLS-1$

}