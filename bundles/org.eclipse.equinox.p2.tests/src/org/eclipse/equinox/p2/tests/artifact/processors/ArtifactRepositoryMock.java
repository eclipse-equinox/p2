/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import java.io.*;
import java.lang.reflect.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Mock the getArtifact() needed to locate the predecessor.
 */
public class ArtifactRepositoryMock implements InvocationHandler {

	private String artifactResource;

	public static IArtifactRepository getMock(String artifactResource) {
		return (IArtifactRepository) Proxy.newProxyInstance(IArtifactRepository.class.getClassLoader(), new Class[] {IArtifactRepository.class}, new ArtifactRepositoryMock(artifactResource));
	}

	private ArtifactRepositoryMock(String artifactResource) {
		this.artifactResource = artifactResource;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (!method.getName().equals("getArtifact"))
			throw new RuntimeException("Unexpected usage!");

		return getArtifact((IArtifactDescriptor) args[0], (OutputStream) args[1], (IProgressMonitor) args[2]);
	}

	private IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		InputStream inputStream;
		try {
			inputStream = TestActivator.getContext().getBundle().getEntry(artifactResource).openStream();
			FileUtils.copyStream(inputStream, true, destination, true);
			return Status.OK_STATUS;
		} catch (IOException e) {
			return new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, ":-(", e);
		}
	}
}