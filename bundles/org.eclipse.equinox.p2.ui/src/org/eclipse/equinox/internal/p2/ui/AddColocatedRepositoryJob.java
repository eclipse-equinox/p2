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
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.p2.operations.AddRepositoryJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;

/**
 * Operation that adds colocated artifact and metadata repositories
 * given a URL.
 * 
 * @since 3.6
 */
public class AddColocatedRepositoryJob extends AddRepositoryJob {
	public AddColocatedRepositoryJob(String label, ProvisioningSession session, URI uri) {
		super(label, session, new URI[] {uri});
	}

	public AddColocatedRepositoryJob(String label, ProvisioningSession session, URI[] uris) {
		super(label, session, uris);
	}

	protected IStatus doBatchedOperation(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, locations.length * 2);

		for (int i = 0; i < locations.length; i++) {
			getSession().addMetadataRepository(locations[i]);
			mon.worked(1);
			getSession().addArtifactRepository(locations[i]);
			mon.worked(1);
		}
		return Status.OK_STATUS;
	}

	protected void setNickname(URI location, String nickname) {
		for (int i = 0; i < locations.length; i++) {
			getSession().setMetadataRepositoryProperty(location, IRepository.PROP_NICKNAME, nickname);
			getSession().setArtifactRepositoryProperty(location, IRepository.PROP_NICKNAME, nickname);
		}
	}
}
