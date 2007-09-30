/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import org.eclipse.core.runtime.IAdaptable;

public class Utils {

	//This method will return the adapter, or will throw an exception
	public static Object getAdapter(IAdaptable toAdapt, Class toAdaptType) throws ProvisioningConfigurationException {
		Object result = toAdapt.getAdapter(toAdaptType);
		if (result == null)
			throw new ProvisioningConfigurationException("Adaptation failure. Can't adapt :" + toAdapt.getClass().getName() + " into a" + toAdaptType.getName());
		return result;
	}

}
