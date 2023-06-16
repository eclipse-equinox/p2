/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - support for remediation page, Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Controls the lifecycle of the provisioning UI bundle
 *
 * @since 3.4
 */
public class ProvUIActivator extends AbstractUIPlugin {
	private static BundleContext context;
	private static ProvUIActivator plugin;
	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.ui"; //$NON-NLS-1$

	private ProvisioningSession session;
	private ProvisioningUI ui;

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the singleton plugin instance
	 *
	 * @return the instance
	 */
	public static ProvUIActivator getDefault() {
		return plugin;
	}

	public ProvUIActivator() {
		// do nothing
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);

		plugin = this;
		ProvUIActivator.context = bundleContext;
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		try {
			// cancel any repository load jobs started in the UI
			Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=305163
			// join the jobs so that this bundle does not stop until the jobs are
			// actually cancelled.
			Job.getJobManager().join(LoadMetadataRepositoryJob.LOAD_FAMILY, new NullProgressMonitor());
			plugin = null;
			ProvUIActivator.context = null;
			ui = null;
		} finally {
			super.stop(bundleContext);
		}
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		createImageDescriptor(ProvUIImages.IMG_METADATA_REPOSITORY, reg);
		createImageDescriptor(ProvUIImages.IMG_ARTIFACT_REPOSITORY, reg);
		createImageDescriptor(ProvUIImages.IMG_IU, reg);
		createImageDescriptor(ProvUIImages.IMG_DISABLED_IU, reg);
		createImageDescriptor(ProvUIImages.IMG_UPDATED_IU, reg);
		createImageDescriptor(ProvUIImages.IMG_ADDED_OVERLAY, reg);
		createImageDescriptor(ProvUIImages.IMG_REMOVED_OVERLAY, reg);
		createImageDescriptor(ProvUIImages.IMG_UPGRADED_IU, reg);
		createImageDescriptor(ProvUIImages.IMG_DOWNGRADED_IU, reg);
		createImageDescriptor(ProvUIImages.IMG_PATCH_IU, reg);
		createImageDescriptor(ProvUIImages.IMG_DISABLED_PATCH_IU, reg);
		createImageDescriptor(ProvUIImages.IMG_CATEGORY, reg);
		createImageDescriptor(ProvUIImages.IMG_PROFILE, reg);
		createImageDescriptor(ProvUIImages.IMG_INFO, reg);
		createImageDescriptor(ProvUIImages.IMG_ADDED, reg);
		createImageDescriptor(ProvUIImages.IMG_REMOVED, reg);
		createImageDescriptor(ProvUIImages.IMG_CHANGED, reg);
		createImageDescriptor(ProvUIImages.IMG_NOTADDED, reg);
		createImageDescriptor(ProvUIImages.IMG_COPY, reg);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL, reg);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_REVERT, reg);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_UNINSTALL, reg);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_UPDATE, reg);
	}

	/**
	 * Creates the specified image descriptor and registers it
	 */
	private void createImageDescriptor(String id, ImageRegistry reg) {
		URL url = FileLocator.find(getBundle(), IPath.fromOSString(ProvUIImages.ICON_PATH + id), null);
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		reg.put(id, desc);
	}

	public ProvisioningUI getProvisioningUI() {
		if (Tracing.DEBUG_DEFAULT_UI)
			Tracing.debug("Falling back to default provisioning UI"); //$NON-NLS-1$

		if (ui == null) {
			IProvisioningAgent agent = ServiceHelper.getService(getContext(), IProvisioningAgent.class);
			session = new ProvisioningSession(agent);
			Policy policy = ServiceHelper.getService(ProvUIActivator.getContext(), Policy.class);
			if (policy == null)
				policy = new Policy();
			ui = new ProvisioningUI(session, IProfileRegistry.SELF, policy);
		}
		return ui;
	}
}
