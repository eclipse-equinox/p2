/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.discovery.wizards;

import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.ui.discovery.util.GradientCanvas;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

/**
 * @author Steffen Pingel
 */
public class CategoryItem<T extends CatalogCategory> extends AbstractDiscoveryItem<T> {

	public CategoryItem(Composite parent, int style, DiscoveryResources resources, T category) {
		super(parent, style, resources, category);
		createContent(category);
	}

	protected boolean hasTooltip(final CatalogCategory category) {
		return category.getOverview() != null && category.getOverview().getSummary() != null && category.getOverview().getSummary().length() > 0;
	}

	private void createContent(T category) {
		setLayout(new FillLayout());

		final GradientCanvas categoryHeaderContainer = new GradientCanvas(this, SWT.NONE);
		categoryHeaderContainer.setSeparatorVisible(true);
		categoryHeaderContainer.setSeparatorAlignment(SWT.TOP);
		categoryHeaderContainer.setBackgroundGradient(new Color[] {resources.getGradientBeginColor(), resources.getGradientEndColor()}, new int[] {100}, true);
		categoryHeaderContainer.putColor(GradientCanvas.H_BOTTOM_KEYLINE1, resources.getGradientBeginColor());
		categoryHeaderContainer.putColor(GradientCanvas.H_BOTTOM_KEYLINE2, resources.getGradientEndColor());

		//GridDataFactory.fillDefaults().span(2, 1).applyTo(categoryHeaderContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).equalWidth(false).applyTo(categoryHeaderContainer);

		Label iconLabel = new Label(categoryHeaderContainer, SWT.NULL);
		if (category.getIcon() != null) {
			iconLabel.setImage(resources.getIconImage(category.getSource(), category.getIcon(), 48, true));
		}
		iconLabel.setBackground(null);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.BEGINNING).span(1, 2).applyTo(iconLabel);

		Label nameLabel = new Label(categoryHeaderContainer, SWT.NULL);
		nameLabel.setFont(resources.getHeaderFont());
		nameLabel.setText(category.getName());
		nameLabel.setBackground(null);

		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameLabel);
		if (hasTooltip(category)) {
			ToolBar toolBar = new ToolBar(categoryHeaderContainer, SWT.FLAT);
			toolBar.setBackground(null);
			ToolItem infoButton = new ToolItem(toolBar, SWT.PUSH);
			infoButton.setImage(resources.getInfoImage());
			infoButton.setToolTipText(Messages.ConnectorDiscoveryWizardMainPage_tooltip_showOverview);
			hookTooltip(toolBar, infoButton, categoryHeaderContainer, nameLabel, category.getSource(), category.getOverview(), null);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(toolBar);
		} else {
			new Label(categoryHeaderContainer, SWT.NULL).setText(" "); //$NON-NLS-1$
		}
		if (category.getDescription() != null) {
			Label description = new Label(categoryHeaderContainer, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).hint(100, SWT.DEFAULT).applyTo(description);
			description.setBackground(null);
			description.setText(category.getDescription());
		}
	}

	@Override
	protected void refresh() {
		// ignore

	}

}
