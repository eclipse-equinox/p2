package org.eclipse.equinox.frameworkadmin;

/**
 * @author iyamasak
 *
 */
public abstract class FrameworkAdminFactory {
	abstract protected FrameworkAdmin createFrameworkAdmin();

	public static FrameworkAdmin getInstance(String className) {
		try {
			FrameworkAdminFactory factory = (FrameworkAdminFactory) Class.forName(className).newInstance();
			return (FrameworkAdmin) factory.createFrameworkAdmin();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
