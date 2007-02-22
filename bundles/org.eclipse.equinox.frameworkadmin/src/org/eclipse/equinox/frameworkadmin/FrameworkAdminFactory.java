package org.eclipse.equinox.frameworkadmin;

public abstract class FrameworkAdminFactory {
	abstract protected FrameworkAdmin createFrameworkAdmin() throws InstantiationException, IllegalAccessException, ClassNotFoundException;

	public static FrameworkAdmin getInstance(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		FrameworkAdminFactory factory = (FrameworkAdminFactory) Class.forName(className).newInstance();
		return (FrameworkAdmin) factory.createFrameworkAdmin();
	}

	//	public static FrameworkAdmin getInstance() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
	//		String className = System.getProperty("org.eclipse.equinox.frameworkadmin.frameworkAdminFactory");
	//		if (className == null)
	//			throw new ClassNotFoundException("System property keyed by \"org.eclipse.equinox.frameworkadmin.frameworkAdminFactory\" is not set.");
	//		FrameworkAdminFactory factory = (FrameworkAdminFactory) Class.forName(className).newInstance();
	//		return (FrameworkAdmin) factory.createFrameworkAdmin();
	//	}

	//	public static FrameworkAdmin getInstance(String className, String configuratorManipulatorFactoryName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
	//		ExtendedFrameworkAdminFactory factory = (ExtendedFrameworkAdminFactory) Class.forName(className).newInstance();
	//		return (FrameworkAdmin) factory.createFrameworkAdmin(configuratorManipulatorFactoryName);
	//	}

}
