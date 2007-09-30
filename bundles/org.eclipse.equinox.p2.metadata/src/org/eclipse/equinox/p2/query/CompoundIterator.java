/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.Messages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.osgi.service.resolver.VersionRange;

public class CompoundIterator implements Iterator {
	private Iterator[] iterators;
	private String id;
	private VersionRange range;
	private RequiredCapability[] requirements;
	private boolean and;
	private int index = 0;
	private boolean filtered = false;
	private IInstallableUnit lookahead = null;

	public static IInstallableUnit[] asArray(Iterator i, IProgressMonitor progress) {
		if (progress == null)
			progress = new NullProgressMonitor();
		progress.beginTask(Messages.QUERY_PROGRESS, IProgressMonitor.UNKNOWN);
		HashSet result = new HashSet();
		while (i.hasNext()) {
			result.add(i.next());
			progress.worked(IProgressMonitor.UNKNOWN);
		}
		progress.done();
		return (IInstallableUnit[]) result.toArray(new IInstallableUnit[result.size()]);
	}

	public CompoundIterator(Iterator[] iterators) {
		this.iterators = iterators;
	}

	public CompoundIterator(Iterator[] iterators, String id, VersionRange range, RequiredCapability[] requirements, boolean and) {
		this(iterators);
		this.id = id;
		this.range = range;
		this.requirements = requirements;
		this.and = and;
		filtered = true;
	}

	public boolean hasNext() {
		if (lookahead != null)
			return true;
		if (index >= iterators.length)
			return false;
		if (filtered) {
			while (iterators[index].hasNext()) {
				IInstallableUnit next = (IInstallableUnit) iterators[index].next();
				if (Query.match(next, id, range, requirements, and)) {
					lookahead = next;
					return true;
				}
			}
		} else {
			if (iterators[index].hasNext())
				return true;
		}
		index++;
		return hasNext();
	}

	public Object next() {
		if (lookahead != null) {
			IInstallableUnit result = lookahead;
			lookahead = null;
			return result;
		}
		if (index >= iterators.length)
			throw new NoSuchElementException();
		while (iterators[index].hasNext()) {
			IInstallableUnit next = (IInstallableUnit) iterators[index].next();
			if (!filtered)
				return next;
			if (Query.match(next, id, range, requirements, and))
				return next;
		}
		index++;
		return next();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
