/*******************************************************************************
 * Copyright (c) 2009, 2017 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     David Green
 *     Shawn Minto bug 275513
 * 	   Steffen Pingel bug 276012 code review, bug 277191 gradient canvas
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.discovery.wizards;

import java.util.List;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * A wizard page that allows users to select connectors that they wish to
 * install.
 * 
 * @see DiscoveryWizard
 * @author David Green
 * @author Steffen Pingel
 */
public class CatalogPage extends WizardPage implements IShellProvider {

	protected static final int MINIMUM_HEIGHT = 480;

	private final Catalog catalog;

	private boolean updated;

	private CatalogViewer viewer;

	public CatalogPage(Catalog catalog) {
		super(CatalogPage.class.getSimpleName());
		this.catalog = catalog;
		setPageComplete(false);
		setTitle(Messages.ConnectorDiscoveryWizardMainPage_connectorDiscovery);
		setDescription(Messages.ConnectorDiscoveryWizardMainPage_pageDescription);
	}

	@Override
	public void createControl(Composite parent) {
		viewer = doCreateViewer(parent);
		viewer.addSelectionChangedListener(event -> setPageComplete(!viewer.getCheckedItems().isEmpty()));
		setControl(viewer.getControl());
	}

	protected CatalogViewer doCreateViewer(Composite parent) {
		CatalogViewer result = new CatalogViewer(getCatalog(), this, getContainer(), getWizard().getConfiguration());
		result.setMinimumHeight(MINIMUM_HEIGHT);
		result.createControl(parent);
		return result;
	}

	protected CatalogViewer getViewer() {
		return viewer;
	}

	protected void doUpdateCatalog() {
		if (!updated) {
			updated = true;
			Display.getCurrent().asyncExec(() -> {
				if (!getControl().isDisposed() && isCurrentPage()) {
					viewer.updateCatalog();
				}
			});
		}
	}

	public Catalog getCatalog() {
		return catalog;
	}

	public List<CatalogItem> getInstallableConnectors() {
		return viewer.getCheckedItems();
	}

	@Override
	public DiscoveryWizard getWizard() {
		return (DiscoveryWizard) super.getWizard();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			doUpdateCatalog();
		}
	}

}
