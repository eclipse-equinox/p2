/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.internal.provisional.frameworkadmin;

import java.lang.reflect.InvocationTargetException;

/**
 * Factory class for creating FrameworkAdmin object from Java programs.
 * 
 * @see FrameworkAdmin
 */
public abstract class FrameworkAdminFactory {
	abstract protected FrameworkAdmin createFrameworkAdmin()
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException;

	// proposed method: only for ConfiguratorManipulatorFactory, magic system
	// property is used.
	public static FrameworkAdmin getInstance(String className)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		FrameworkAdminFactory factory = (FrameworkAdminFactory) Class.forName(className).getDeclaredConstructor()
				.newInstance();
		return factory.createFrameworkAdmin();
	}

	// // method 3: two magic system properties are used.
	// public static FrameworkAdmin getInstance() throws InstantiationException,
	// IllegalAccessException, ClassNotFoundException {
	// String className =
	// System.getProperty("org.eclipse.equinox.internal.provisional.frameworkadmin.frameworkAdminFactory");
	// if (className == null)
	// throw new ClassNotFoundException("System property keyed by
	// \"org.eclipse.equinox.internal.provisional.frameworkadmin.frameworkAdminFactory\"
	// is not set.");
	// FrameworkAdminFactory factory = (FrameworkAdminFactory)
	// Class.forName(className).newInstance();
	// return (FrameworkAdmin) factory.createFrameworkAdmin();
	// }

	// // method 1: no magic system properties are used.
	//
	// public static FrameworkAdmin getInstance(String className, String
	// configuratorManipulatorFactoryName) throws InstantiationException,
	// IllegalAccessException, ClassNotFoundException {
	// ExtendedFrameworkAdminFactory factory = (ExtendedFrameworkAdminFactory)
	// Class.forName(className).newInstance();
	// return (FrameworkAdmin)
	// factory.createFrameworkAdmin(configuratorManipulatorFactoryName);
	// }

}
