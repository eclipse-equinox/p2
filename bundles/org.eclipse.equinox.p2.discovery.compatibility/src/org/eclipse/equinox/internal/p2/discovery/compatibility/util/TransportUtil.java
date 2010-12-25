/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Sonatype Inc. - transport split
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.compatibility.util;

import java.io.*;
import java.net.URI;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.transport.ecf.RepositoryTransport;

/**
 * A utility for accessing web resources
 * 
 * @author David Green
 */
public class TransportUtil {

	/**
	 * Extend to process character content.
	 * 
	 * @see TransportUtil#readResource(URI, TextContentProcessor, IProgressMonitor)
	 */
	public interface TextContentProcessor {

		public void process(Reader reader) throws IOException;

	}

	/**
	 * Download an HTTP-based resource
	 * 
	 * @param target
	 *            the target file to which the content is saved
	 * @param location
	 *            the web location of the content
	 * @param monitor
	 *            the monitor
	 * @throws IOException
	 *             if a network or IO problem occurs
	 */
	public static void downloadResource(URI location, File target, IProgressMonitor monitor) throws IOException {
		OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
		try {
			new RepositoryTransport().download(location, out, monitor);
		} finally {
			out.close();
		}
	}

	/**
	 * Read a web-based resource at the specified location using the given processor.
	 * 
	 * @param location
	 *            the web location of the content
	 * @param processor
	 *            the processor that will handle content
	 * @param monitor
	 *            the monitor
	 * @throws IOException
	 *             if a network or IO problem occurs
	 * @throws CoreException
	 */
	public static void readResource(URI location, TextContentProcessor processor, IProgressMonitor monitor) throws IOException, CoreException {
		InputStream in = new RepositoryTransport().stream(location, monitor);
		try {
			// FIXME how can the charset be determined?
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8")); //$NON-NLS-1$
			processor.process(reader);
		} finally {
			in.close();
		}
	}

	/**
	 * Verify availability of resources at the given web locations. Normally this would be done using an HTTP HEAD.
	 * 
	 * @param locations
	 *            the locations of the resource to verify
	 * @param one
	 *            indicate if only one of the resources must exist
	 * @param monitor
	 *            the monitor
	 * @return true if the resource exists
	 * @throws CoreException
	 * @throws AuthenticationFailedException
	 */
	public static boolean verifyAvailability(List<? extends URI> locations, boolean one, IProgressMonitor monitor) throws IOException, CoreException {
		if (locations.isEmpty() || locations.size() > 5) {
			throw new IllegalArgumentException();
		}
		int countFound = 0;
		for (URI location : locations) {
			try {
				new RepositoryTransport().getLastModified(location, monitor);
				if (one) {
					return true;
				}
				++countFound;
			} catch (FileNotFoundException e) {
				if (!one) {
					return false;
				}
				continue;
			}
		}
		return countFound == locations.size();
	}

}
