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
package org.eclipse.equinox.internal.p2.ui.query;

import java.net.URI;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElementCollector;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;

/**
 * Collector that accepts the matched repo URLs and
 * wraps them in a MetadataRepositoryElement.
 * 
 * @since 3.4
 */
public class MetadataRepositoryElementCollector extends QueriedElementCollector {

	public MetadataRepositoryElementCollector(IQueryable queryable, Object parent) {
		super(queryable, parent);
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		if (!(match instanceof URI))
			return true;
		MetadataRepositoryElement element = new MetadataRepositoryElement(parent, (URI) match, ProvisioningUtil.getMetadataRepositoryEnablement((URI) match));
		try {
			String nickname = ProvisioningUtil.getMetadataRepositoryProperty((URI) match, IRepository.PROP_NICKNAME);
			if (nickname != null && nickname.length() > 0)
				element.setNickname(nickname);
		} catch (ProvisionException e) {
			// swallow
		}
		return super.accept(element);
	}
}
