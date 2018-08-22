/*******************************************************************************
 * Copyright (c) 2007, 2018 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *   IBM - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;

public class VersionlessArtifactKey extends ArtifactKey {

	public VersionlessArtifactKey(String classifier, String id) {
		super(classifier, id, Version.emptyVersion);
	}

	public VersionlessArtifactKey(IArtifactKey base) {
		super(base.getClassifier(), base.getId(), Version.emptyVersion);
	}

	@Override
	public int hashCode() {
		int hash = getId().hashCode();
		hash = 17 * hash + getClassifier().hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IArtifactKey))
			return false;
		IArtifactKey ak = (IArtifactKey) obj;
		return ak.getId().equals(getId()) && ak.getClassifier().equals(getClassifier());
	}
}