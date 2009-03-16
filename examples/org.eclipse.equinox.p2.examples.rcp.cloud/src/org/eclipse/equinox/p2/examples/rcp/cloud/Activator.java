package org.eclipse.equinox.p2.examples.rcp.cloud;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.ui.ProfileFactory;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IProfileChooser;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.examples.rcp.cloud";

	// The shared instance
	private static Activator plugin;
	
	// The update site used for this product
	private static final String CLOUD_UPDATE_SITE = "http://www.eclipse.org/equinox/p2/testing/updateSite";
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		initializeP2Policies();
		initializeP2Repositories();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	private void initializeP2Policies() {
		Policy policy = Policy.getDefault();
		// Where a profile must be chosen, use the running profile
		policy.setProfileChooser(new IProfileChooser() {
			public String getProfileId(Shell shell) {
				return ProfileFactory.makeProfile("Canned").getProfileId();
			}
		});
		// User has no access to manipulate repositories
		policy.setRepositoryManipulator(null);
	}
	
	// This should really be done at product build time
	private void initializeP2Repositories() {
		try {
			URI uri = URIUtil.fromString(CLOUD_UPDATE_SITE);
			ProvisioningUtil.addMetadataRepository(uri, false);
			ProvisioningUtil.addArtifactRepository(uri, false);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null, StatusManager.SHOW | StatusManager.LOG);
		} catch (URISyntaxException e) {
			ProvUI.handleException(e, null, StatusManager.SHOW | StatusManager.LOG);
		}
	}
}
