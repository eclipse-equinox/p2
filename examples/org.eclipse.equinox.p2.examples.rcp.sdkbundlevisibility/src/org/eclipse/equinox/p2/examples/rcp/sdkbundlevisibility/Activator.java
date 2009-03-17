package org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility;

import org.eclipse.equinox.internal.provisional.p2.ui.ProfileFactory;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.ColocatedRepositoryManipulator;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IProfileChooser;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility.p2.PreferenceConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility";

	// The shared instance
	private static Activator plugin;
	
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
		initializeP2Policies();
		plugin = this;
	}
	
	private void initializeP2Policies() {
		Policy policy = Policy.getDefault();
		// XXX Use the pref-based repository manipulator
		policy.setRepositoryManipulator(new ColocatedRepositoryManipulator(policy, PreferenceConstants.PREF_PAGE_SITES));
		
		// XXX Change the visibility of the IUs shown in the UI.
        // Using a null property for visibility means everything will be shown, not just
        // groups (features).
		IUViewQueryContext context = policy.getQueryContext();
		context.setVisibleAvailableIUProperty(null);
		context.setVisibleInstalledIUProperty(null);
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
}
