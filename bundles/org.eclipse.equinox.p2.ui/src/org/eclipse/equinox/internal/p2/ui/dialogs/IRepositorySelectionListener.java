/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.net.URI;

/**
 * Listener for the repository selection combo.  Whenever the selected repository changes (menu selection, 
 * text modified, new repo added) this listener will be notified.
 * 
 * @since 3.5
 */
public interface IRepositorySelectionListener {
	/**
	 * Called whenever the selected repository in the combo changes.
	 * 
	 * @param repoChoice one of AvailableIUGroup.AVAILABLE_NONE, AvailableIUGroup.AVAILABLE_ALL, AvailableIUGroup.AVAILABLE_LOCAL, AvailableIUGroup.AVAILABLE_SPECIFIED  
	 * @param repoLocation if the repoChoice is set to AvailableIUGroup.AVAILABLE_SPECIFIED, this field will contain the URI of the selected repo, otherwise <code>null</code>
	 */
	public void repositorySelectionChanged(int repoChoice, URI repoLocation);
}