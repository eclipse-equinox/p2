/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.repository.IRepository;

public class RepositoryFileSet extends FileSet {
	public final static int TYPE_ARTIFACT = IRepository.TYPE_ARTIFACT;
	public final static int TYPE_METADATA = IRepository.TYPE_METADATA;

	private int kind = RepositoryDescriptor.TYPE_BOTH;
	private boolean optional = false;
	protected String myLocation = null;

	public void setKind(String repoKind) {
		kind = RepositoryDescriptor.determineKind(repoKind);
	}

	public int getKind() {
		return kind;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public boolean isOptional() {
		return optional;
	}

	public boolean isBoth() {
		return kind == RepositoryDescriptor.TYPE_BOTH;
	}

	public boolean isArtifact() {
		return kind == RepositoryDescriptor.TYPE_BOTH || kind == IRepository.TYPE_ARTIFACT;
	}

	public boolean isMetadata() {
		return kind == RepositoryDescriptor.TYPE_BOTH || kind == IRepository.TYPE_METADATA;
	}

	public void setLocation(String value) {
		myLocation = value;
	}

	public String getRepoLocation() {
		return myLocation;
	}

	public URI getRepoLocationURI() {
		try {
			return RepositoryHelper.localRepoURIHelper(URIUtil.fromString(getRepoLocation()));
		} catch (URISyntaxException e) {
			throw new BuildException(e);
		}
	}
}
