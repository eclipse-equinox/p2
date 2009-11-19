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
package org.eclipse.equinox.p2.operations;

import java.net.URI;
import org.eclipse.core.runtime.Assert;

/**
 * Abstract class representing an operation that adds repositories,
 * using an optional nickname.  Subclasses determine which types
 * of repositories are being added.
 * 
 * @since 2.0
 */
public abstract class AddRepositoryJob extends SignallingRepositoryJob {

	private String[] nicknames;

	/**
	 * Create a repository which will add the specified repository locations.
	 * 
	 * @param name the name of the job
	 * @param session the provisioning session used for the job
	 * @param locations the locations of the repositories to be added
	 */
	public AddRepositoryJob(String name, ProvisioningSession session, URI[] locations) {
		super(name, session, locations);
	}

	/**
	 * Set the nicknames that should be associated with the repository locations.
	 * 
	 * @param nicknames An array of nicknames which is the same length as the repository
	 * locations.  Each nickname should be a string name that corresponds to the URI location 
	 * at the same position in the locations array.
	 */
	public void setNicknames(String[] nicknames) {
		Assert.isLegal(nicknames != null && nicknames.length == locations.length);
		this.nicknames = nicknames;
	}

	/**
	 * Set the nickname for the specified repository in the repository manager.
	 * 
	 * @param location the location of the repository
	 * @param nickname the nickname to use for the repository
	 */
	protected abstract void setNickname(URI location, String nickname);

	/**
	 * Return the array of nicknames corresponding to the repository locations.
	 * 
	 * @return 	 An array of nicknames which is the same length as the repository
	 * locations, or <code>null</code> if no nicknames have been specified.  
	 * If an array is specified, each nickname should be a string that corresponds 
	 * to the URI location at the same position in the locations array.

	 */
	protected String[] getNicknames() {
		return nicknames;
	}
}
