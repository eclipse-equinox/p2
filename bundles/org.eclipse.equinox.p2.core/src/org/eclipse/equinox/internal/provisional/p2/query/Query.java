/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.query;

import java.util.Iterator;

/**
 * The superclass of all queries that can be performed on an {@link IQueryable}.
 * <p>
 * 
 * <B>NOTE:  This interface does not follow the proper naming convention. It should 
 * be IQuery, however, for historic reasons it is Query.  This is likely to change.</B>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface Query {

	/**
	 * Evaluates the query for a specific input.  
	 * 
	 * @param iterator The elements for which to evaluate the query on
	 * @param result A collector to collect the results.  For each element accepted 
	 * by the query,{@link Collector#accept(Object)} must be called.
	 * @return The results of the query.  The collector returned must be
	 * the collector passed in.
	 */
	public abstract Collector perform(Iterator iterator, Collector result);

	/**
	 * Gets the ID for this Query. 
	 */
	public String getId();

	/**
	 * Gets a particular property of the query.
	 * @param property The property to retrieve 
	 */
	public Object getProperty(String property);
}
