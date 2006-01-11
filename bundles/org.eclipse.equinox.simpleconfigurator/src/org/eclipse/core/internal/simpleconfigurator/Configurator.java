/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.simpleconfigurator;

import org.eclipse.core.simpleConfigurator.Installer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Configurator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		new Installer().applyConfiguration(context);
	}

	public void stop(BundleContext context) throws Exception {
	}

}
