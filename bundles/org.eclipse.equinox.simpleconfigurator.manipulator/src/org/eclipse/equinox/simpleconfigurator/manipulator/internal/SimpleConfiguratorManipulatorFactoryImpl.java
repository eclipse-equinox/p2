package org.eclipse.equinox.simpleconfigurator.manipulator.internal;

import org.eclipse.equinox.configuratormanipulator.ConfiguratorManipulator;
import org.eclipse.equinox.configuratormanipulator.ConfiguratorManipulatorFactory;

public class SimpleConfiguratorManipulatorFactoryImpl extends ConfiguratorManipulatorFactory {

	protected ConfiguratorManipulator createConfiguratorManipulator() {
		return new SimpleConfiguratorManipulatorImpl();
	}

}
