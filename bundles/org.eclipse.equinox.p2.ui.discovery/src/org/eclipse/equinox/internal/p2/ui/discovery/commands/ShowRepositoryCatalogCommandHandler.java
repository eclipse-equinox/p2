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

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.commands.*;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.ui.discovery.repository.RepositoryDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * A command that causes the {@link CatalogWizard} to appear in a dialog.
 * 
 * @author Steffen Pingel
 */
public class ShowRepositoryCatalogCommandHandler extends AbstractHandler {

	private static final String DEFAULT_REPOSITORY_URL = "http://download.eclipse.org/tools/mylyn/update/e3.4"; //$NON-NLS-1$

	private static final String ID_P2_INSTALL_UI = "org.eclipse.equinox.p2.ui.sdk/org.eclipse.equinox.p2.ui.sdk.install"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {
		// check to make sure that the p2 install ui is enabled
		if (WorkbenchUtil.allowUseOf(ID_P2_INSTALL_UI)) {
			Catalog catalog = new Catalog();

			// look for descriptors from installed bundles
			RepositoryDiscoveryStrategy strategy = new RepositoryDiscoveryStrategy();
			try {
				strategy.addLocation(new URI(DEFAULT_REPOSITORY_URL));
			} catch (URISyntaxException e) {
				throw new ExecutionException("Invalid location format", e); //$NON-NLS-1$
			}
			catalog.getDiscoveryStrategies().add(strategy);

			catalog.setEnvironment(DiscoveryCore.createEnvironment());
			catalog.setVerifyUpdateSiteAvailability(false);

			CatalogConfiguration configuration = new CatalogConfiguration();
			configuration.setShowTagFilter(false);

			DiscoveryWizard wizard = new DiscoveryWizard(catalog, configuration);
			WizardDialog dialog = new WizardDialog(WorkbenchUtil.getShell(), wizard);
			dialog.open();
		} else {
			MessageDialog.openWarning(WorkbenchUtil.getShell(), Messages.ShowConnectorDiscoveryWizardCommandHandler_Install_Connectors, Messages.ShowConnectorDiscoveryWizardCommandHandler_Unable_To_Install_No_P2);
		}

		return null;
	}
}
