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
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.internal.repository.tools.Repo2Runnable;

/**
 * Ant task which calls the "repo to runnable" application. This application takes an
 * existing p2 repository (local or remote), iterates over its list of IUs, and fetches 
 * all of the corresponding artifacts to a user-specified location. Once fetched, the
 * artifacts will be in "runnable" form... that is directory-based bundles will be
 * extracted into folders and packed JAR files will be un-packed.
 * 
 * @since 1.0
 */
public class Repo2RunnableTask extends Task {

	private final Repo2Runnable application;
	private List iuTasks = new ArrayList();
	private List sourceRepos = new ArrayList();

	/*
	 * Constructor for the class. Create a new instance of the application
	 * so we can populate it with attributes.
	 */
	public Repo2RunnableTask() {
		super();
		this.application = new Repo2Runnable();
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			prepareSourceRepos();
			prepareIUs();
			IStatus result = application.run(null);
			if (result.matches(IStatus.ERROR))
				throw new ProvisionException(result);
		} catch (ProvisionException e) {
			throw new BuildException("Error occurred while transforming repository.", e);
		} catch (URISyntaxException e) {
			throw new BuildException("Error occurred while transforming repository.", e);
		}
	}

	/*
	 * If the user specified some source repositories via sub-elements
	 * then add them to the transformer for consideration.
	 */
	private void prepareSourceRepos() {
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
							URI uri = new File(fileset.getDir(), elements[i][j]).toURI();
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

	private void prepareIUs() throws URISyntaxException {
		if (iuTasks == null || iuTasks.isEmpty())
			return;
		CompositeMetadataRepository repository = new CompositeMetadataRepository(new URI("memory:/composite"), "parent metadata repo", null);
		for (Iterator iter = application.getSourceMetadataRepositories().iterator(); iter.hasNext();) {
			repository.addChild((URI) iter.next());
		}
		List result = new ArrayList();
		for (Iterator iter = iuTasks.iterator(); iter.hasNext();) {
			IUTask iu = (IUTask) iter.next();
			String id = iu.getId();
			Version version = new Version(iu.getVersion());
			Collector collector = new Collector();
			repository.query(new InstallableUnitQuery(id, version), collector, null);
			if (collector.isEmpty())
				System.err.println("Unable to find " + id + " " + version);
			else
				result.add(collector.iterator().next());
		}
		application.setSourceIUs(result);
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
		application.setDestinationArtifactRepository(location);
		application.setDestinationMetadataRepository(location);
	}

	/*
	 * Create an object to hold IU information since the user specified an "iu" sub-element.
	 */
	public Object createIu() {
		IUTask iu = new IUTask();
		iuTasks.add(iu);
		return iu;
	}

	/*
	 * Create a special file set since the user specified a "source" sub-element.
	 */
	public FileSet createSource() {
		MyFileSet set = new MyFileSet();
		sourceRepos.add(set);
		return set;
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
}
