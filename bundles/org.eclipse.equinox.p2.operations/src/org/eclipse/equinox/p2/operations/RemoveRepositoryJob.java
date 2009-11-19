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

/**
 * Abstract class representing an operation that removes repositories.
 * 
 * @since 2.0
 */
public abstract class RemoveRepositoryJob extends SignallingRepositoryJob {

	/** 
	 * Create a job that removes the specified repositories.
	 * @param name the name of the job
	 * @param session the provisioning session used to retrieve services
	 * @param locations the locations of the repositories to be removed
	 */
	public RemoveRepositoryJob(String name, ProvisioningSession session, URI[] locations) {
		super(name, session, locations);
	}

}
