/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.io.File;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;

public class JREAction extends AbstractPublishingAction {

	private File location;

	public JREAction(IPublisherInfo info, File location) {
		this.location = location;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		IArtifactDescriptor artifact = MetadataGeneratorHelper.createJREData(location, results);
		if (artifact != null)
			publishArtifact(artifact, new File[] {location}, info, INCLUDE_ROOT);
		return Status.OK_STATUS;
	}

}
