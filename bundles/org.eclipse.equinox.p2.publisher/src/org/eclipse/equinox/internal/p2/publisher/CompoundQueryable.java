/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.provisional.p2.query.*;

public class CompoundQueryable implements IQueryable {

	IQueryable[] children;

	public CompoundQueryable(IQueryable[] children) {
		this.children = children;
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, children.length * 10);
		for (int i = 0; i < children.length; i++)
			children[i].query(query, collector, sub.newChild(1));
		sub.done();
		return collector;
	}
}
