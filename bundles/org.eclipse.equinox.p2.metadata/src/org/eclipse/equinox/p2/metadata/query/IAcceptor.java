/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.metadata.query;

/**
 * An IAcceptor processes objects that satisfy a query.
 * 
 * @since 2.0
 *
 */
public interface IAcceptor {

	/**
	 * Accepts an object.
	 * <p>
	 * This default implementation adds the objects to a list. Clients may
	 * override this method to perform additional filtering, add different objects 
	 * to the list, short-circuit the traversal, or process the objects directly without 
	 * collecting them.
	 * 
	 * @param object the object to collect or visit
	 * @return <code>true</code> if the traversal should continue,
	 * or <code>false</code> to indicate the traversal should stop.
	 */
	public boolean accept(Object object);
}
