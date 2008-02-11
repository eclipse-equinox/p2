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
package org.eclipse.equinox.internal.simpleconfigurator.manipulator;

import org.eclipse.equinox.internal.provisional.configuratormanipulator.ConfiguratorManipulator;
import org.eclipse.equinox.internal.provisional.configuratormanipulator.ConfiguratorManipulatorFactory;

public class SimpleConfiguratorManipulatorFactoryImpl extends ConfiguratorManipulatorFactory {

	protected ConfiguratorManipulator createConfiguratorManipulator() {
		return new SimpleConfiguratorManipulatorImpl();
	}

}
