/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.mirror;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

public class Mirror {
	private IArtifactRepository source;
	private IArtifactRepository destination;
	private boolean raw;

	public Mirror(IArtifactRepository source, IArtifactRepository destination, boolean raw) {
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
				source.getArtifact(descriptor, repositoryStream, new NullProgressMonitor());
			} finally {
				if (repositoryStream != null)
					repositoryStream.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
