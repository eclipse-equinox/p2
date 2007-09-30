/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.Messages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.osgi.service.resolver.VersionRange;

public class QueryableArray implements IQueryable {
	private IInstallableUnit[] source;

	public QueryableArray(IInstallableUnit[] source) {
		this.source = source;
	}

	public Iterator getIterator(String id, VersionRange range, RequiredCapability[] requirements, boolean and) {
		Iterator i = Arrays.asList(source).iterator();
		return new CompoundIterator(new Iterator[] {i}, id, range, requirements, and);
	}

	public IInstallableUnit[] query(String id, VersionRange range, RequiredCapability[] requirements, boolean and, IProgressMonitor progress) {
		if (progress == null)
			progress = new NullProgressMonitor();
		progress.beginTask(Messages.QUERY_PROGRESS, source.length);
		Set result = new HashSet();
		for (int i = 0; i < source.length; i++) {
			if (Query.match(source[i], id, range, requirements, and))
				result.add(source[i]);
			progress.worked(1);
		}
		progress.done();
		return (IInstallableUnit[]) result.toArray(new IInstallableUnit[result.size()]);

	}
}
