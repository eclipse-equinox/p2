/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.query;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.model.RollbackProfileElement;

/**
 * Collector that examines available IU's and wraps them in an
 * element representing either a category an IU.
 *  
 * @since 3.4
 */
public class RollbackIUCollector extends AvailableIUCollector {

	public RollbackIUCollector(IQueryProvider queryProvider, IQueryable queryable) {
		super(queryProvider, queryable, false);
	}

	protected Object makeDefaultElement(IInstallableUnit iu) {
		return new RollbackProfileElement(iu);
	}
}
