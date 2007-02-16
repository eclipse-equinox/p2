package org.eclipse.equinox.frameworkadmin.equinox.internal;

import org.eclipse.equinox.frameworkadmin.FrameworkAdminFactory;
import org.eclipse.equinox.frameworkadmin.FrameworkAdmin;
import org.osgi.framework.BundleContext;

public class EquinoxFrameworkAdminFactoryImpl extends FrameworkAdminFactory {
	public FrameworkAdmin createFrameworkAdmin() {
		return new EquinoxFwAdminImpl(null);
	}
}
