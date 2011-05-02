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
package org.eclipse.equinox.internal.p2.ui.discovery.wizards;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryImages;
import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryUi;
import org.eclipse.equinox.internal.p2.ui.discovery.operations.DiscoveryInstallOperation;
import org.eclipse.jface.wizard.Wizard;

/**
 * A wizard for performing discovery and selecting items from a catalog to install. When finish is pressed, selected
 * items are downloaded and installed.
 * 
 * @see DiscoveryInstallOperation
 * @see CatalogPage
 * @author David Green
 * @author Steffen Pingel
 */
public class DiscoveryWizard extends Wizard {

	private final Catalog catalog;

	private CatalogPage catalogPage;

	private final CatalogConfiguration configuration;

	public DiscoveryWizard(Catalog catalog, CatalogConfiguration configuration) {
		Assert.isNotNull(catalog);
		Assert.isNotNull(configuration);
		this.catalog = catalog;
		this.configuration = configuration;
		setWindowTitle(Messages.DiscoveryWizard_Install_Window_Title);
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(DiscoveryImages.BANNER_DISOVERY);
	}

	@Override
	public void addPages() {
		addPage(getCatalogPage());
	}

	protected CatalogPage doCreateCatalogPage() {
		return new CatalogPage(getCatalog());
	}

	public Catalog getCatalog() {
		return catalog;
	}

	public CatalogPage getCatalogPage() {
		if (catalogPage == null) {
			catalogPage = doCreateCatalogPage();
		}
		return catalogPage;
	}

	public CatalogConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public boolean performFinish() {
		return DiscoveryUi.install(catalogPage.getInstallableConnectors(), getContainer());
	}

}
