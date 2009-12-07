/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.mirror;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * A utility class that performs mirroring of metadatas between repositories.
 */
public class Mirroring {

	public void validate(IMetadataRepository source, IMetadataRepository destination) {
		if (source == null)
			throw new IllegalStateException("Source repository is null."); //$NON-NLS-1$
		if (destination == null)
			throw new IllegalStateException("Destination repository is null."); //$NON-NLS-1$
		if (!destination.isModifiable())
			throw new IllegalStateException("Destination repository must be modifiable: " + destination.getLocation()); //$NON-NLS-1$
	}

	public void mirror(IMetadataRepository source, IMetadataRepository destination, String[] rootSpecs, boolean transitive) {
		if (rootSpecs == null)
			mirror(source, destination, InstallableUnitQuery.ANY, transitive);
		else {
			VersionRangedName[] roots = new VersionRangedName[rootSpecs.length];
			for (int i = 0; i < rootSpecs.length; i++)
				roots[i] = VersionRangedName.parse(rootSpecs[i]);
			mirror(source, destination, new RangeQuery(roots), transitive);
		}
	}

	public void mirror(IMetadataRepository source, IMetadataRepository destination, IQuery query, boolean transitive) {
		validate(source, destination);
		Collector result = source.query(query, new Collector(), null);
		mirror(source, destination, (IInstallableUnit[]) result.toArray(IInstallableUnit.class), transitive);
	}

	private void mirror(IMetadataRepository source, IMetadataRepository destination, IInstallableUnit[] roots, boolean transitive) {
		if (transitive)
			roots = addTransitiveIUs(source, roots);
		destination.addInstallableUnits(roots);
	}

	protected IInstallableUnit[] addTransitiveIUs(IMetadataRepository source, IInstallableUnit[] roots) {
		// TODO Here we should create a profile from the source repo and discover all the 
		// IUs that are needed to support the given roots.  For now just assume that the 
		// given roots are enough.
		return roots;
	}
}
