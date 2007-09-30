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
package org.eclipse.equinox.prov.engine;

public class ProvisioningConfigurationException extends RuntimeException {

	private static final long serialVersionUID = -712627437440533809L;

	public ProvisioningConfigurationException(String name) {
		super(name);
	}

}
