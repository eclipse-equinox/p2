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
package org.eclipse.equinox.p2.metadata.query;

import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;

/**
 * The superclass of all queries that can be performed on an {@link IQueryable}.
 * <p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IQuery {

	/**
	 * Evaluates the query for a specific input.  
	 * 
	 * @param iterator The elements for which to evaluate the query on
	 * @return The results of the query.  The collector returned must be
	 * the collector passed in.
	 */
	public abstract IQueryResult perform(Iterator iterator);

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
