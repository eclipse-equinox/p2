/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import org.eclipse.equinox.p2.repository.tools.comparator.ArtifactComparatorFactory;
import org.eclipse.equinox.p2.repository.tools.comparator.IArtifactComparator;

import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.*;

public class ArtifactRepositoryValidator {

	private IArtifactComparator comparator;

	public ArtifactRepositoryValidator(String comparatorId) throws ProvisionException {
		comparator = ArtifactComparatorFactory.getArtifactComparator(comparatorId);
		if (comparatorId == null)
			throw new ProvisionException(Messages.invalidComparatorId);

	}

	public IStatus validateRepository(IArtifactRepository repository) {
		if (repository instanceof CompositeArtifactRepository)
			return validateComposite((CompositeArtifactRepository) repository);

		IQueryResult<IArtifactKey> queryResult = repository.query(ArtifactKeyQuery.ALL_KEYS, new NullProgressMonitor());
		for (Iterator<IArtifactKey> iterator = queryResult.iterator(); iterator.hasNext();) {
			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(iterator.next());
			for (int i = 0; i < descriptors.length - 2; i++) {
				IStatus compareResult = comparator.compare(repository, descriptors[i], repository, descriptors[i + 1]);
				if (!compareResult.isOK()) {
					return compareResult;
				}
			}
		}
		return Status.OK_STATUS;
	}

	public IStatus validateComposite(CompositeArtifactRepository repository) {
		List<IArtifactRepository> repos = repository.getLoadedChildren();
		IQueryResult<IArtifactKey> queryResult = repository.query(ArtifactKeyQuery.ALL_KEYS, new NullProgressMonitor());
		for (Iterator<IArtifactKey> iterator = queryResult.iterator(); iterator.hasNext();) {
			IArtifactKey key = iterator.next();
			IArtifactRepository firstRepo = null;
			for (IArtifactRepository child : repos) {
				if (child.contains(key)) {
					if (firstRepo == null) {
						firstRepo = child;
						continue;
					}

					IArtifactDescriptor[] d1 = firstRepo.getArtifactDescriptors(key);
					IArtifactDescriptor[] d2 = child.getArtifactDescriptors(key);
					//If we assume each repo is internally consistant, we only need to compare one descriptor from each repo
					IStatus compareResult = comparator.compare(firstRepo, d1[0], child, d2[0]);
					if (!compareResult.isOK()) {
						//LogHelper.log(compareResult);
						return compareResult;
					}
				}
			}
		}
		return Status.OK_STATUS;
	}

	public IStatus validateComposite(CompositeArtifactRepository composite, IArtifactRepository repository) {
		IQueryResult<IArtifactKey> queryResult = repository.query(ArtifactKeyQuery.ALL_KEYS, new NullProgressMonitor());
		for (Iterator<IArtifactKey> iterator = queryResult.iterator(); iterator.hasNext();) {
			IArtifactKey key = iterator.next();
			if (composite.contains(key)) {
				IArtifactDescriptor[] d1 = composite.getArtifactDescriptors(key);
				IArtifactDescriptor[] d2 = repository.getArtifactDescriptors(key);
				//If we assume each repo is internally consistant, we only need to compare one descriptor from each repo
				IStatus compareResult = comparator.compare(composite, d1[0], repository, d2[0]);
				if (!compareResult.isOK())
					return compareResult;
			}
		}
		return Status.OK_STATUS;
	}
}
