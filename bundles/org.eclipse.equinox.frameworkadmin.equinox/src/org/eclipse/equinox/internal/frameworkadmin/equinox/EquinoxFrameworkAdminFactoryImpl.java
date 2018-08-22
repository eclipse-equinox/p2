/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.equinox.internal.provisional.configuratormanipulator.ConfiguratorManipulatorFactory;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdmin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdminFactory;

public class EquinoxFrameworkAdminFactoryImpl extends FrameworkAdminFactory {
	@Override
	public FrameworkAdmin createFrameworkAdmin()
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		String className = System.getProperty(ConfiguratorManipulatorFactory.SYSTEM_PROPERTY_KEY);
		if (className == null)
			return new EquinoxFwAdminImpl();
		return new EquinoxFwAdminImpl(className);
	}
}
