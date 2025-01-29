/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;

public class DestinationRepository extends DataType {

	private final RepositoryDescriptor descriptor = new RepositoryDescriptor();

	public void setCompressed(boolean compress) {
		descriptor.setCompressed(compress);
	}

	public void setName(String repoName) {
		descriptor.setName(repoName);
	}

	public void setLocation(String repoLocation) throws BuildException {
		try {
			descriptor.setLocation(URIUtil.fromString(repoLocation));
		} catch (URISyntaxException e) {
			throw new BuildException(e);
		}
	}

	public void setFormat(String formatLocation) {
		try {
			descriptor.setFormat(URIUtil.fromString(formatLocation));
		} catch (URISyntaxException e) {
			throw new BuildException(e);
		}
	}

	public void setAppend(boolean appendMode) {
		descriptor.setAppend(appendMode);
	}

	public void setKind(String repoKind) {
		descriptor.setKind(repoKind);
	}

	public void setAtomic(String atomicAsBoolean) {
		descriptor.setAtomic(atomicAsBoolean);
	}

	RepositoryDescriptor getDescriptor() {
		return descriptor;
	}
}
