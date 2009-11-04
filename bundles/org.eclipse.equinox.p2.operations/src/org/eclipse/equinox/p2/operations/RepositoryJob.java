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
 * Abstract class representing provisioning repository jobs
 * 
 * @since 2.0
 */
public abstract class RepositoryJob extends ProvisioningJob {

	protected URI[] locations;

	public RepositoryJob(String label, ProvisioningSession session, URI[] locations) {
		super(label, session);
		this.locations = locations;
	}

	public boolean runInBackground() {
		return true;
	}

}
