package org.eclipse.equinox.frameworkadmin;

/**
 * Factory class for creating FrameworkAdmin object from Java programs.
 * 
 *  @see FrameworkAdmin
 */
public abstract class FrameworkAdminFactory {
	abstract protected FrameworkAdmin createFrameworkAdmin() throws InstantiationException, IllegalAccessException, ClassNotFoundException;

	// proposed method: only for ConfiguratorManipulatorFactory, magic system property is used.
	public static FrameworkAdmin getInstance(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		FrameworkAdminFactory factory = (FrameworkAdminFactory) Class.forName(className).newInstance();
		return (FrameworkAdmin) factory.createFrameworkAdmin();
	}

	//  // method 3: two magic system properties are used.
	//	public static FrameworkAdmin getInstance() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
	//		String className = System.getProperty("org.eclipse.equinox.frameworkadmin.frameworkAdminFactory");
	//		if (className == null)
	//			throw new ClassNotFoundException("System property keyed by \"org.eclipse.equinox.frameworkadmin.frameworkAdminFactory\" is not set.");
	//		FrameworkAdminFactory factory = (FrameworkAdminFactory) Class.forName(className).newInstance();
	//		return (FrameworkAdmin) factory.createFrameworkAdmin();
	//	}

	//  // method 1: no magic system properties are used.
	//
	//	public static FrameworkAdmin getInstance(String className, String configuratorManipulatorFactoryName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
	//		ExtendedFrameworkAdminFactory factory = (ExtendedFrameworkAdminFactory) Class.forName(className).newInstance();
	//		return (FrameworkAdmin) factory.createFrameworkAdmin(configuratorManipulatorFactoryName);
	//	}

}
