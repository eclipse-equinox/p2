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

import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.types.FileSet;

public class RepositoryList extends RepositoryFileSet {
	// TODO this class should extend DataType, currently RepoFileSet to support <source location="xxx" /> 
	List<DestinationRepository> repositories = new ArrayList<DestinationRepository>();
	List<FileSet> sourceFileSets = new ArrayList<FileSet>();

	public DestinationRepository createRepository() {
		DestinationRepository repo = new DestinationRepository();
		repositories.add(repo);
		return repo;
	}

	public RepositoryFileSet createFileSet() {
		RepositoryFileSet fileSet = new RepositoryFileSet();
		sourceFileSets.add(fileSet);
		return fileSet;
	}

	public List<DestinationRepository> getRepositoryList() {
		return repositories;
	}

	public List<FileSet> getFileSetList() {
		//TODO this should eventually be removed
		sourceFileSets.add(this);
		return sourceFileSets;
	}
}
