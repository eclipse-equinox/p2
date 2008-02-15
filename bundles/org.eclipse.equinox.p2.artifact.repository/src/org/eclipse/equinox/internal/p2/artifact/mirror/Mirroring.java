/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *		compeople AG (Stefan Liebig) - various ongoing maintenance
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.mirror;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

/**
 * A utility class that performs mirroring of artifacts between repositories.
 */
public class Mirroring {
	private IArtifactRepository source;
	private IArtifactRepository destination;
	private boolean raw;

	public Mirroring(IArtifactRepository source, IArtifactRepository destination, boolean raw) {
		this.source = source;
		this.destination = destination;
		this.raw = raw;
	}

	public void run() {
		if (!destination.isModifiable())
			throw new IllegalStateException("Destination repository must be modifiable: " + destination.getLocation());
		IArtifactKey[] keys = source.getArtifactKeys();
		for (int i = 0; i < keys.length; i++) {
			IArtifactKey key = keys[i];
			IArtifactDescriptor[] descriptors = source.getArtifactDescriptors(key);
			for (int j = 0; j < descriptors.length; j++)
				mirror(descriptors[j]);
		}
	}

	private void mirror(IArtifactDescriptor descriptor) {
		IArtifactDescriptor newDescriptor = raw ? descriptor : new ArtifactDescriptor(descriptor);
		try {
			OutputStream repositoryStream = null;
			try {
				System.out.println("Mirroring: " + descriptor.getArtifactKey()); //$NON-NLS-1$
				repositoryStream = destination.getOutputStream(newDescriptor);
				if (repositoryStream == null)
					return;
				// TODO Is that ok to ignore the result?
				source.getArtifact(descriptor, repositoryStream, new NullProgressMonitor());
			} finally {
				if (repositoryStream != null)
					repositoryStream.close();
			}
		} catch (ProvisionException e) {
			// TODO Is that ok to ignore the exception
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Is that ok to ignore the exception
			e.printStackTrace();
		}
	}

}
