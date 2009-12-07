/******************************************************************************* 
* Copyright (c) 2008, 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import org.eclipse.equinox.p2.metadata.query.IQuery;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRequirement {

	public int getMin();

	public int getMax();

	public IQuery getMatches();

	public IQuery getFilter();

	public boolean isGreedy();
}