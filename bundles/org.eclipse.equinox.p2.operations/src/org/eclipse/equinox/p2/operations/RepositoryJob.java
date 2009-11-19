/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.net.URI;

/**
 * Abstract class representing jobs that manipulate 
 * provisioning repositories.
 * 
 * @since 2.0
 */
public abstract class RepositoryJob extends ProvisioningJob {

	protected URI[] locations;

	/**
	 * Create a repository job that can be used to manipulate the specified 
	 * repository locations.
	 * @param name the name of the job
	 * @param session the provisioning session to be used
	 * @param locations the locations of the repositories to be manipulated.
	 */
	protected RepositoryJob(String name, ProvisioningSession session, URI[] locations) {
		super(name, session);
		this.locations = locations;
	}
}
