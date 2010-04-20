/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;

/**
 * A command that causes the {@link DiscoveryWizard} to appear in a dialog.
 * 
 * @author Steffen Pingel
 */
public class ShowRepositoryCatalogCommandHandler extends AbstractHandler {

	private static final String ID_PARAMETER_REPOSITORY = "org.eclipse.equinox.p2.ui.discovery.commands.RepositoryParameter"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String location = event.getParameter(ID_PARAMETER_REPOSITORY);
		if (location == null) {
			throw new ExecutionException(NLS.bind(Messages.ShowRepositoryCatalogCommandHandler_Required_parameter_not_specified_Error, ID_PARAMETER_REPOSITORY));
		}
		URI uri;
		try {
			uri = new URI(location);
		} catch (URISyntaxException e) {
			throw new ExecutionException(Messages.ShowRepositoryCatalogCommandHandler_Location_not_valid_Error, e);
		}

		Catalog catalog = new Catalog();

		RepositoryDiscoveryStrategy strategy = new RepositoryDiscoveryStrategy();
		strategy.addLocation(uri);
		catalog.getDiscoveryStrategies().add(strategy);

		catalog.setEnvironment(DiscoveryCore.createEnvironment());
		catalog.setVerifyUpdateSiteAvailability(false);

		CatalogConfiguration configuration = new CatalogConfiguration();
		configuration.setShowTagFilter(false);

		DiscoveryWizard wizard = new DiscoveryWizard(catalog, configuration);
		WizardDialog dialog = new WizardDialog(WorkbenchUtil.getShell(), wizard);
		dialog.open();

		return null;
	}

}
