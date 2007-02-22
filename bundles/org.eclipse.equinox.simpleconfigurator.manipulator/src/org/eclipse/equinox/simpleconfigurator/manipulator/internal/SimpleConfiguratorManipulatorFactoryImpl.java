package org.eclipse.equinox.simpleconfigurator.manipulator.internal;

import org.eclipse.equinox.configurator.ConfiguratorManipulator;
import org.eclipse.equinox.configurator.ConfiguratorManipulatorFactory;

public class SimpleConfiguratorManipulatorFactoryImpl extends ConfiguratorManipulatorFactory {

	protected ConfiguratorManipulator createConfiguratorManipulator() {
		return new SimpleConfiguratorManipulatorImpl();
	}

}
