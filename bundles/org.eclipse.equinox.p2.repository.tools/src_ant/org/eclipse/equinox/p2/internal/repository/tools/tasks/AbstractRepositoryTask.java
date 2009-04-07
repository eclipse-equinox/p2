/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.FileSet;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.p2.internal.repository.tools.AbstractApplication;

public abstract class AbstractRepositoryTask extends Task {
	protected AbstractApplication application;
	protected List iuTasks = new ArrayList();
	protected List sourceRepos = new ArrayList();
	protected List destinations = new ArrayList();

	/*
	  * Create a special file set since the user specified a "source" sub-element.
	  */
	public FileSet createSource() {
		MyFileSet set = new MyFileSet();
		sourceRepos.add(set);
		return set;
	}

	protected void addMetadataSourceRepository(URI repoLocation) {
		application.addSourceMetadataRepository(repoLocation);
	}

	protected void addArtifactSourceRepository(URI repoLocation) {
		application.addSourceArtifactRepository(repoLocation);
	}

	/*
	 * Create an object to hold IU information since the user specified an "iu" sub-element.
	 */
	public Object createIu() {
		IUDescription iu = new IUDescription();
		iuTasks.add(iu);
		return iu;
	}

	/*
	 * If the repositories are co-located then the user just has to set one
	 * argument to specify both the artifact and metadata repositories.
	 */
	public void setSource(String location) {
		application.addSourceArtifactRepository(location);
		application.addSourceMetadataRepository(location);
	}

	/*
	 * If the repositories are co-located then the user just has to set one
	 * argument to specify both the artifact and metadata repositories.
	 */
	public void setDestination(String location) {
		DestinationRepository metadata = new DestinationRepository();
		metadata.setLocation(URIUtil.toUnencodedString(new Path(location).toFile().toURI()));
		metadata.setKind("metadata"); //$NON-NLS-1$
		application.addDestination(metadata.getDescriptor());
		destinations.add(metadata);

		DestinationRepository artifact = new DestinationRepository();
		artifact.setLocation(URIUtil.toUnencodedString(new Path(location).toFile().toURI()));
		metadata.setKind("artifact"); //$NON-NLS-1$
		application.addDestination(artifact.getDescriptor());
		destinations.add(artifact);
	}

	public DestinationRepository createDestination() {
		DestinationRepository destination = new DestinationRepository();
		destinations.add(destination);
		application.addDestination(destination.getDescriptor());
		return destination;
	}

	/*
	 * New FileSet subclass which adds an optional "location" attribute.
	 */
	public class MyFileSet extends FileSet {
		String location;

		public MyFileSet() {
			super();
		}

		public void setLocation(String value) {
			this.location = value;
		}
	}

	/*
	 * If the user specified some source repositories via sub-elements
	 * then add them to the transformer for consideration.
	 */
	protected void prepareSourceRepos() {
		if (sourceRepos == null || sourceRepos.isEmpty())
			return;
		for (Iterator iter = sourceRepos.iterator(); iter.hasNext();) {
			Object next = iter.next();
			if (next instanceof MyFileSet) {
				MyFileSet fileset = (MyFileSet) next;
				// determine if the user set a "location" attribute or used a fileset
				if (fileset.location == null) {
					DirectoryScanner scanner = fileset.getDirectoryScanner(getProject());
					String[][] elements = new String[][] {scanner.getIncludedDirectories(), scanner.getIncludedFiles()};
					for (int i = 0; i < 2; i++) {
						for (int j = 0; j < elements[i].length; j++) {
							File file = new File(fileset.getDir(), elements[i][j]);
							URI uri = file.toURI();

							if (file.isFile() && file.getName().endsWith(".zip")) { //$NON-NLS-1$
								try {
									uri = new URI("jar:" + uri.toString() + "!/"); //$NON-NLS-1$ //$NON-NLS-2$
								} catch (URISyntaxException e) {
									//?
									continue;
								}
							}
							application.addSourceArtifactRepository(uri);
							application.addSourceMetadataRepository(uri);
						}
					}
				} else {
					application.addSourceArtifactRepository(fileset.location);
					application.addSourceMetadataRepository(fileset.location);
				}
			}
		}
	}

	protected List prepareIUs() {
		if (iuTasks == null || iuTasks.isEmpty())
			return null;

		IMetadataRepository repository = application.getCompositeMetadataRepository();
		List result = new ArrayList();
		for (Iterator iter = iuTasks.iterator(); iter.hasNext();) {
			IUDescription iu = (IUDescription) iter.next();
			Query iuQuery = iu.createQuery();
			Collector collector = new Collector();

			repository.query(iuQuery, collector, null);

			if (iu.isRequired() && collector.isEmpty())
				throw new BuildException("Unable to find: " + iu.toString()); //$NON-NLS-1$ 
			result.addAll(collector.toCollection());
		}
		return result;
	}
}
