/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.p2.metadata.query.IQuery;

/**
 * A test queryable that contains a simple collection of objects.
 */
public class MockQueryable implements IQueryable {
	private final Collection items;

	public MockQueryable() {
		this(new ArrayList());
	}

	public MockQueryable(Object item) {
		this(new ArrayList());
		this.items.add(item);
	}

	public MockQueryable(Collection items) {
		this.items = items;

	}

	public Collector query(IQuery query, IProgressMonitor monitor) {
		Collector collector = new Collector();
		return query.perform(items.iterator(), collector);
	}

}
