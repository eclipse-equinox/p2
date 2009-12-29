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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.internal.repository.tools.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractRepositoryTask extends Task {
	protected static final String ANT_PREFIX = "${"; //$NON-NLS-1$
	protected AbstractApplication application;
	protected List<IUDescription> iuTasks = new ArrayList<IUDescription>();
	protected List<FileSet> sourceRepos = new ArrayList<FileSet>();
	protected List<DestinationRepository> destinations = new ArrayList<DestinationRepository>();

	protected void addMetadataSourceRepository(URI repoLocation) {
		RepositoryDescriptor source = new RepositoryDescriptor();
		source.setLocation(repoLocation);
		source.setKind(RepositoryDescriptor.KIND_METADATA);
		application.addSource(source);
	}

	protected void addArtifactSourceRepository(URI repoLocation) {
		RepositoryDescriptor source = new RepositoryDescriptor();
		source.setLocation(repoLocation);
		source.setKind(RepositoryDescriptor.KIND_ARTIFACT);
		application.addSource(source);
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
		RepositoryDescriptor source = new RepositoryDescriptor();
		try {
			source.setLocation(URIUtil.fromString(location));
			application.addSource(source);
		} catch (URISyntaxException e) {
			throw new BuildException(e);
		}
	}

	/*
	 * If the repositories are co-located then the user just has to set one
	 * argument to specify both the artifact and metadata repositories.
	 */
	public void setDestination(String location) {
		// TODO depreciate 
		DestinationRepository dest = new DestinationRepository();
		dest.setLocation(location);
		destinations.add(dest);
		application.addDestination(dest.getDescriptor());
	}

	public DestinationRepository createDestination() {
		// TODO depreciate 
		DestinationRepository destination = new DestinationRepository();
		destinations.add(destination);
		application.addDestination(destination.getDescriptor());
		return destination;
	}

	/*
	 * Add a repository to mirror into
	 */
	public DestinationRepository createRepository() {
		DestinationRepository destination = new DestinationRepository();
		destinations.add(destination);
		application.addDestination(destination.getDescriptor());
		return destination;
	}

	/*
	 * Add source repositories to mirror from
	 */
	public void addConfiguredSource(RepositoryList sourceList) {
		for (Iterator<DestinationRepository> iter = sourceList.getRepositoryList().iterator(); iter.hasNext();) {
			DestinationRepository repo = iter.next();
			application.addSource(repo.getDescriptor());
		}

		for (Iterator<FileSet> iter = sourceList.getFileSetList().iterator(); iter.hasNext();) {
			FileSet fileSet = iter.next();
			sourceRepos.add(fileSet);
			// Added to the application later through prepareSourceRepos
		}
	}

	/*
	 * If the user specified some source repositories via sub-elements
	 * then add them to the transformer for consideration.
	 */
	protected void prepareSourceRepos() {
		if (sourceRepos == null || sourceRepos.isEmpty())
			return;
		for (Iterator<FileSet> iter = sourceRepos.iterator(); iter.hasNext();) {
			RepositoryFileSet fileset = (RepositoryFileSet) iter.next();

			if (fileset.getRepoLocation() != null) {
				//TODO depreciate
				if (!fileset.getRepoLocation().startsWith(ANT_PREFIX)) {
					addArtifactSourceRepository(fileset.getRepoLocationURI());
					addMetadataSourceRepository(fileset.getRepoLocationURI());
				}
			} else if (fileset.getDir() != null) {
				DirectoryScanner scanner = fileset.getDirectoryScanner(getProject());
				String[][] elements = new String[][] {scanner.getIncludedDirectories(), scanner.getIncludedFiles()};
				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < elements[i].length; j++) {
						File file = new File(fileset.getDir(), elements[i][j]);
						URI uri = file.toURI();

						if (file.isFile() && file.getName().endsWith(".zip")) { //$NON-NLS-1$
							uri = URIUtil.toJarURI(uri, null);
						}
						if (fileset.isBoth()) {
							addArtifactSourceRepository(uri);
							addMetadataSourceRepository(uri);
						} else if (fileset.isArtifact())
							addArtifactSourceRepository(uri);
						else if (fileset.isMetadata())
							addMetadataSourceRepository(uri);
						else
							throw new BuildException(NLS.bind(Messages.unknown_repository_type, uri));
					}
				}
			}
		}
		sourceRepos.clear();
	}

	protected List<IInstallableUnit> prepareIUs() {
		if (iuTasks == null || iuTasks.isEmpty())
			return null;

		IMetadataRepository repository = application.getCompositeMetadataRepository();
		List<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
		for (Iterator<IUDescription> iter = iuTasks.iterator(); iter.hasNext();) {
			IUDescription iu = iter.next();
			IQuery<IInstallableUnit> iuQuery = iu.createQuery();

			Iterator<IInstallableUnit> queryResult = repository.query(iuQuery, null).iterator();

			if (iu.isRequired() && !queryResult.hasNext())
				throw new BuildException(NLS.bind(Messages.AbstractRepositoryTask_unableToFind, iu.toString()));
			while (queryResult.hasNext())
				result.add(queryResult.next());
		}
		return result;
	}

	protected void log(IStatus status) {
		try {
			new AntMirrorLog(this).log(status);
		} catch (NoSuchMethodException e) {
			// Shouldn't occur
		}
	}
}
