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

import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;

/**
 * @author David Green
 */
public abstract class DiscoveryCore {

	public static final String ID_PLUGIN = "org.eclipse.equinox.p2.discovery"; //$NON-NLS-1$

	private DiscoveryCore() {
	}

	public static Dictionary<Object, Object> createEnvironment() {
		Dictionary<Object, Object> environment = new Hashtable<Object, Object>(System.getProperties());
		// add the installed Mylyn version to the environment so that we can
		// have
		// connectors that are filtered based on version of Mylyn
		IProduct product = Platform.getProduct();
		if (product != null) {
			environment.put("org.eclipse.product.id", product.getId()); //$NON-NLS-1$
		}
		return environment;
	}

}
