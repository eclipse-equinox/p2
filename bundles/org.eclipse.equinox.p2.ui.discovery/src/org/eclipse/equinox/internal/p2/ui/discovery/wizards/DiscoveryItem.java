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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Overview;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * @author Steffen Pingel
 */
public class DiscoveryItem<T extends CatalogItem> extends AbstractDiscoveryItem<T> implements PropertyChangeListener {

	private Button checkbox;

	private Composite checkboxContainer;

	private final CatalogItem connector;

	private Label description;

	private Label iconLabel;

	private ToolItem infoButton;

	private Label nameLabel;

	private Link providerLabel;

	private final IShellProvider shellProvider;

	private ToolItem updateButton;

	private final CatalogViewer viewer;

	public DiscoveryItem(Composite parent, int style, DiscoveryResources resources, IShellProvider shellProvider, final T connector, CatalogViewer viewer) {
		super(parent, style, resources, connector);
		this.shellProvider = shellProvider;
		this.connector = connector;
		this.viewer = viewer;
		connector.addPropertyChangeListener(this);
		this.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				connector.removePropertyChangeListener(DiscoveryItem.this);
			}
		});
		createContent();
		initializeListeners();
	}

	private void createContent() {
		GridLayout layout = new GridLayout(4, false);
		layout.marginLeft = 7;
		layout.marginTop = 2;
		layout.marginBottom = 2;
		setLayout(layout);

		checkboxContainer = new Composite(this, SWT.INHERIT_NONE);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.BEGINNING).span(1, 2).applyTo(checkboxContainer);
		GridLayoutFactory.fillDefaults().spacing(1, 1).numColumns(2).applyTo(checkboxContainer);

		checkbox = new Button(checkboxContainer, SWT.CHECK | SWT.INHERIT_FORCE);
		checkbox.setSelection(connector.isSelected());
		checkbox.setText(" "); //$NON-NLS-1$
		// help UI tests
		checkbox.setData("connectorId", connector.getId()); //$NON-NLS-1$
		// FIXME
		//		checkbox.addFocusListener(new FocusAdapter() {
		//			@Override
		//			public void focusGained(FocusEvent e) {
		//				bodyScrolledComposite.showControl(this);
		//			}
		//		});
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(checkbox);

		iconLabel = new Label(checkboxContainer, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(iconLabel);
		if (connector.getIcon() != null) {
			iconLabel.setImage(resources.getIconImage(connector.getSource(), connector.getIcon(), 32, false));
		}

		nameLabel = new Label(this, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(nameLabel);
		nameLabel.setFont(resources.getSmallHeaderFont());
		nameLabel.setText(connector.getName());

		providerLabel = new Link(this, SWT.RIGHT);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(providerLabel);
		// always disabled color to make it less prominent
		providerLabel.setForeground(resources.getColorDisabled());
		if (connector.getCertification() != null) {
			providerLabel.setText(NLS.bind(Messages.DiscoveryViewer_Certification_Label0, new String[] {connector.getProvider(), connector.getLicense(), connector.getCertification().getName()}));
			if (connector.getCertification().getUrl() != null) {
				providerLabel.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						WorkbenchUtil.openUrl(connector.getCertification().getUrl(), IWorkbenchBrowserSupport.AS_EXTERNAL);
					}
				});
			}
			Overview overview = new Overview();
			overview.setSummary(connector.getCertification().getDescription());
			overview.setUrl(connector.getCertification().getUrl());
			Image image = resources.getIconImage(connector.getSource(), connector.getCertification().getIcon(), 48, true);
			hookTooltip(providerLabel, providerLabel, this, providerLabel, connector.getSource(), overview, image);
		} else {
			providerLabel.setText(NLS.bind(Messages.ConnectorDiscoveryWizardMainPage_provider_and_license, connector.getProvider(), connector.getLicense()));
		}

		if (hasTooltip(connector) || connector.isInstalled()) {
			ToolBar toolBar = new ToolBar(this, SWT.FLAT);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(toolBar);

			if (connector.isInstalled()) {
				updateButton = new ToolItem(toolBar, SWT.PUSH);
				updateButton.setImage(resources.getUpdateImage());
				updateButton.setToolTipText("Check for Updates");
			}

			if (hasTooltip(connector)) {
				infoButton = new ToolItem(toolBar, SWT.PUSH);
				infoButton.setImage(resources.getInfoImage());
				infoButton.setToolTipText(Messages.ConnectorDiscoveryWizardMainPage_tooltip_showOverview);
				hookTooltip(toolBar, infoButton, this, nameLabel, connector.getSource(), connector.getOverview(), null);
			}
		} else {
			Label label = new Label(this, SWT.NULL);
			label.setText(" "); //$NON-NLS-1$
		}

		description = new Label(this, SWT.NULL | SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).hint(100, SWT.DEFAULT).applyTo(description);
		String descriptionText = connector.getDescription();
		int maxDescriptionLength = 162;
		if (descriptionText == null) {
			descriptionText = ""; //$NON-NLS-1$
		}
		if (descriptionText.length() > maxDescriptionLength) {
			descriptionText = descriptionText.substring(0, maxDescriptionLength);
		}
		description.setText(descriptionText.replaceAll("(\\r\\n)|\\n|\\r", " ")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected boolean hasTooltip(final CatalogItem connector) {
		return connector.getOverview() != null && connector.getOverview().getSummary() != null && connector.getOverview().getSummary().length() > 0;
	}

	public void initializeListeners() {
		checkbox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				boolean selected = checkbox.getSelection();
				maybeModifySelection(selected);
			}
		});
		MouseListener connectorItemMouseListener = new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				boolean selected = !checkbox.getSelection();
				if (maybeModifySelection(selected)) {
					checkbox.setSelection(selected);
				}
			}
		};
		checkboxContainer.addMouseListener(connectorItemMouseListener);
		this.addMouseListener(connectorItemMouseListener);
		iconLabel.addMouseListener(connectorItemMouseListener);
		nameLabel.addMouseListener(connectorItemMouseListener);
		// the provider has clickable links
		//providerLabel.addMouseListener(connectorItemMouseListener);
		description.addMouseListener(connectorItemMouseListener);
	}

	protected boolean maybeModifySelection(boolean selected) {
		if (selected) {
			if (connector.isInstalled()) {
				MessageDialog.openWarning(shellProvider.getShell(), "Install Connector", NLS.bind("{0} is already installed.", connector.getName()));
				return false;
			}
			if (connector.getAvailable() != null && !connector.getAvailable()) {
				MessageDialog.openWarning(shellProvider.getShell(), Messages.ConnectorDiscoveryWizardMainPage_warningTitleConnectorUnavailable, NLS.bind(Messages.ConnectorDiscoveryWizardMainPage_warningMessageConnectorUnavailable, connector.getName()));
				return false;
			}
		}
		viewer.modifySelection(connector, selected);
		return true;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (!isDisposed()) {
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!isDisposed()) {
						refresh();
					}
				}
			});
		}
	}

	@Override
	protected void refresh() {
		boolean enabled = connector.getAvailable() == null || connector.getAvailable();

		checkbox.setEnabled(!connector.isInstalled() && enabled);
		nameLabel.setEnabled(connector.isInstalled() || enabled);
		providerLabel.setEnabled(connector.isInstalled() || enabled);
		description.setEnabled(connector.isInstalled() || enabled);
		Color foreground;
		if (connector.isInstalled() || enabled) {
			foreground = getForeground();
		} else {
			foreground = resources.getColorDisabled();
		}
		nameLabel.setForeground(foreground);
		description.setForeground(foreground);
	}

}
