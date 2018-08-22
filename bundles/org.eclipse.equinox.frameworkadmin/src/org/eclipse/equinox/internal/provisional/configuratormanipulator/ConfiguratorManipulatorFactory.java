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
package org.eclipse.equinox.internal.provisional.configuratormanipulator;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdmin;

/**
 * Factory class for creating ConfiguratorManipulator object from Java programs.
 * 
 * @see FrameworkAdmin
 */
public abstract class ConfiguratorManipulatorFactory {
	public final static String SYSTEM_PROPERTY_KEY = "org.eclipse.equinox.configuratorManipulatorFactory"; //$NON-NLS-1$

	abstract protected ConfiguratorManipulator createConfiguratorManipulator();

	public static ConfiguratorManipulator getInstance(String className)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		ConfiguratorManipulatorFactory factory = (ConfiguratorManipulatorFactory) Class.forName(className)
				.getDeclaredConstructor().newInstance();
		return factory.createConfiguratorManipulator();
	}
}
