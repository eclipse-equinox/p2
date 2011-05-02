/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *   IBM - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers;

import org.eclipse.equinox.p2.metadata.Version;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

public class VersionlessArtifactKey extends ArtifactKey {

	public VersionlessArtifactKey(String classifier, String id) {
		super(classifier, id, Version.emptyVersion);
	}

	public VersionlessArtifactKey(IArtifactKey base) {
		super(base.getClassifier(), base.getId(), Version.emptyVersion);
	}

	public int hashCode() {
		int hash = getId().hashCode();
		hash = 17 * hash + getClassifier().hashCode();
		return hash;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof IArtifactKey))
			return false;
		IArtifactKey ak = (IArtifactKey) obj;
		return ak.getId().equals(getId()) && ak.getClassifier().equals(getClassifier());
	}
}