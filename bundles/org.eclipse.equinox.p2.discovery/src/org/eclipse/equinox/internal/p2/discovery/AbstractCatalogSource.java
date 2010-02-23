/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery;

import java.net.URL;

/**
 * @author David Green
 */
public abstract class AbstractCatalogSource {

	private Policy policy = Policy.defaultPolicy();

	/**
	 * an identifier that can be used to determine the origin of the source, typically for logging purposes.
	 */
	public abstract Object getId();

	/**
	 * get a resource by an URL relative to the root of the source.
	 * 
	 * @param resourceName
	 *            the relative resource name
	 * @return an URL to the resource, or null if it is known that the resource does not exist.
	 */
	public abstract URL getResource(String resourceName);

	public Policy getPolicy() {
		return policy;
	}

	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

}
