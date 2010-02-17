/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.discovery.commands;

import java.util.Arrays;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.compatibility.*;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * A command that causes the {@link CatalogWizard} to appear in a dialog.
 * 
 * @author David Green
 */
public class ShowBundleCatalogCommandHandler extends AbstractHandler {

	private static final String DEFAULT_DIRECTORY_URL = "http://www.eclipse.org/mylyn/discovery/directory-3.3.xml"; //$NON-NLS-1$

	private static final String SYSTEM_PROPERTY_DIRECTORY_URL = "mylyn.discovery.directory"; //$NON-NLS-1$

	private static final String ID_P2_INSTALL_UI = "org.eclipse.equinox.p2.ui.sdk/org.eclipse.equinox.p2.ui.sdk.install"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) {
		// check to make sure that the p2 install ui is enabled
		if (WorkbenchUtil.allowUseOf(ID_P2_INSTALL_UI)) {
			Catalog catalog = new Catalog();
			catalog.setTags(Arrays.asList(ConnectorDiscoveryExtensionReader.TAGS));

			// look for descriptors from installed bundles
			catalog.getDiscoveryStrategies().add(new BundleDiscoveryStrategy());

			// look for remote descriptor
			String directoryUrl = System.getProperty(SYSTEM_PROPERTY_DIRECTORY_URL, DEFAULT_DIRECTORY_URL);
			if (directoryUrl.length() > 0) {
				RemoteBundleDiscoveryStrategy remoteDiscoveryStrategy = new RemoteBundleDiscoveryStrategy();
				remoteDiscoveryStrategy.setDirectoryUrl(directoryUrl);
				catalog.getDiscoveryStrategies().add(remoteDiscoveryStrategy);
			}

			catalog.setEnvironment(DiscoveryCore.createEnvironment());
			catalog.setVerifyUpdateSiteAvailability(false);

			CatalogConfiguration configuration = new CatalogConfiguration();
			configuration.setShowTagFilter(true);
			configuration.setSelectedTags(catalog.getTags());

			DiscoveryWizard wizard = new DiscoveryWizard(catalog, configuration);
			WizardDialog dialog = new WizardDialog(WorkbenchUtil.getShell(), wizard);
			dialog.open();
		} else {
			MessageDialog.openWarning(WorkbenchUtil.getShell(), Messages.ShowConnectorDiscoveryWizardCommandHandler_Install_Connectors, Messages.ShowConnectorDiscoveryWizardCommandHandler_Unable_To_Install_No_P2);
		}

		return null;
	}
}
