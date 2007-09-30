/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.query;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.prov.metadata.Messages;
import org.eclipse.equinox.prov.metadata.*;
import org.eclipse.osgi.service.resolver.VersionRange;

public class Query {

	public static boolean match(IInstallableUnit source, String id, VersionRange range, RequiredCapability[] requirements, boolean and) {
		if (id != null && !source.getId().equals(id))
			return false;
		if (range != null && !range.isIncluded(source.getVersion()))
			return false;
		if (requirements == null)
			return true;
		// if any of the requirements are met by any of the capabilities of the source...
		for (int k = 0; k < requirements.length; k++) {
			boolean valid = false;
			ProvidedCapability[] capabilities = source.getProvidedCapabilities();
			for (int j = 0; j < capabilities.length; j++)
				//TODO Need to deal with option and multiplicity flag
				if ((requirements[k].getName().equals(capabilities[j].getName()) && requirements[k].getNamespace().equals(capabilities[j].getNamespace()) && (requirements[k].getRange() == null ? true : requirements[k].getRange().isIncluded(capabilities[j].getVersion())))) {
					valid = true;
				}
			// if we are OR'ing then the first time we find a requirement that is met, return success
			if (valid && !and)
				return true;
			// if we are AND'ing then the first time we find a requirement that is NOT met, return failure
			if (!valid && and)
				return false;
		}
		// if we get past the requirements check and we are AND'ing then return true 
		// since all requirements must have been met.  If we are OR'ing then return false 
		// since none of the requirements were met.
		return and;
	}

	public static IInstallableUnit[] query(IQueryable[] sources, String id, VersionRange range, RequiredCapability[] requirements, boolean and, IProgressMonitor progress) {
		if (progress == null)
			progress = new NullProgressMonitor();

		progress.beginTask(Messages.QUERY_PROGRESS, sources.length);
		ArrayList result = new ArrayList();
		for (int i = 0; i < sources.length; i++) {
			IQueryable source = sources[i];
			IInstallableUnit[] list = source.query(id, range, requirements, and, progress);
			for (int j = 0; j < list.length; j++)
				result.add(list[j]);
			progress.worked(1);
		}
		progress.done();
		return (IInstallableUnit[]) result.toArray(new IInstallableUnit[result.size()]);
	}

	public static Iterator getIterator(IQueryable[] sources, String id, VersionRange range, RequiredCapability[] requirements, boolean and) {
		Iterator[] result = new Iterator[sources.length];
		for (int i = 0; i < sources.length; i++)
			result[i] = sources[i].getIterator(id, range, requirements, and);
		return new CompoundIterator(result);
	}
}
