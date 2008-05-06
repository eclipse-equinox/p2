package org.eclipse.equinox.internal.p2.extensionlocation;

import java.io.File;
import java.net.URL;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.osgi.framework.*;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.extensionlocation"; //$NON-NLS-1$null;
	private static volatile BundleContext bundleContext;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}

	public static BundleContext getContext() {
		return bundleContext;
	}

	public static IProfile getCurrentProfile() {
		ServiceReference reference = bundleContext.getServiceReference(IProfileRegistry.class.getName());
		if (reference == null)
			return null;
		IProfileRegistry profileRegistry = (IProfileRegistry) bundleContext.getService(reference);
		try {
			return profileRegistry.getProfile(IProfileRegistry.SELF);
		} finally {
			bundleContext.ungetService(reference);
		}
	}

	public static IFileArtifactRepository getBundlePoolRepository() {
		ServiceReference reference = bundleContext.getServiceReference(IProfileRegistry.class.getName());
		if (reference == null)
			return null;
		IProfileRegistry profileRegistry = (IProfileRegistry) bundleContext.getService(reference);
		IProfile profile = null;
		try {
			profile = profileRegistry.getProfile(IProfileRegistry.SELF);
		} finally {
			bundleContext.ungetService(reference);
		}
		if (profile == null)
			return null;

		return Util.getBundlePoolRepository(profile);
	}

	/**
	 * Returns a reasonable human-readable repository name for the given location.
	 */
	public static String getRepositoryName(URL location) {
		File file = URLUtil.toFile(location);
		return file == null ? location.toExternalForm() : file.getAbsolutePath();
	}
}
