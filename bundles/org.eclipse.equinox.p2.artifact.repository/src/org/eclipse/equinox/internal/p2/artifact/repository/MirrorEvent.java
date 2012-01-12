/*******************************************************************************
 * Copyright (c) 2012 Wind River and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.util.EventObject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

public class MirrorEvent extends EventObject {

	private static final long serialVersionUID = -2958057566692590943L;

	private IStatus downloadStatus;

	private IArtifactRepository repository;

	private IArtifactDescriptor descriptor;

	public MirrorEvent(IArtifactRepository repo, IArtifactDescriptor descriptor, IStatus downloadStatus) {
		super(descriptor);
		this.repository = repo;
		this.descriptor = descriptor;
		this.downloadStatus = downloadStatus;
	}

	public IArtifactRepository getSourceRepository() {
		return repository;
	}

	public IArtifactDescriptor getMirroredDescriptor() {
		return descriptor;
	}

	public IStatus getDownloadStatus() {
		return downloadStatus;
	}
}
