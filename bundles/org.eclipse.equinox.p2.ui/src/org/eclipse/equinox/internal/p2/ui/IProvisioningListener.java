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
package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.equinox.p2.core.eventbus.ProvisioningListener;

/**
 * A tagging listener used to distinguish listeners
 * for events that aren't triggered in the core.
 * 
 * @since 3.4
 */
public interface IProvisioningListener extends ProvisioningListener {

	public final static String REPO_ADDED = "org.eclipse.equinox.p2.ui.property.repoadded"; //$NON-NLS-1$
	public final static String REPO_REMOVED = "org.eclipse.equinox.p2.ui.property.reporemoved"; //$NON-NLS-1$
}