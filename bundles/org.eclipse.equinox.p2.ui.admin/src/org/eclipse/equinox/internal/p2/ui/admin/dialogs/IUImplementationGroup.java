/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;

import java.util.Collection;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * An IUImplementationGroup is a reusable UI component that displays and edits the 
 * implementation-oriented properties of an IU. It can be used in 
 * different dialogs that manipulate or define IU's.
 * 
 * @since 3.4
 */
public class IUImplementationGroup extends IUGroup {

	private Text id;
	private Text version;
	private Text namespace;
	private Text touchpointType;
	private List touchpointData;
	private List requiredCapabilities;
	private List providedCapabilities;

	public IUImplementationGroup(final Composite parent, IInstallableUnit iu, ModifyListener listener) {
		super(parent, iu, listener);
	}

	protected Composite createGroupComposite(Composite parent, ModifyListener listener) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.widthHint = 350;
		composite.setLayoutData(data);

		// Grid data for text controls
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);

		// Grid data for controls spanning both columns
		GridData gd2 = new GridData(GridData.FILL_HORIZONTAL);
		gd2.horizontalSpan = 2;

		// Grid data for lists grabbing vertical space
		GridData gdList = new GridData(GridData.FILL_HORIZONTAL);
		GC gc = new GC(parent);
		gc.setFont(JFaceResources.getDialogFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		gdList.horizontalSpan = 2;
		gdList.heightHint = Dialog.convertHeightInCharsToPixels(fontMetrics, 5);

		boolean editable = iuElement == null && listener != null;

		Label label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.IUGroup_ID);
		id = new Text(composite, SWT.BORDER);
		id.setLayoutData(gd);
		if (editable) {
			id.addModifyListener(listener);
		} else {
			id.setEditable(false);
		}

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.IUGroup_Version);
		version = new Text(composite, SWT.BORDER);
		version.setLayoutData(gd);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.IUGroup_Namespace);
		namespace = new Text(composite, SWT.BORDER);
		namespace.setLayoutData(gd);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.IUGroup_TouchpointType);
		touchpointType = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		touchpointType.setLayoutData(gd);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.IUGroup_TouchpointData);
		label.setLayoutData(gd2);
		touchpointData = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		touchpointData.setLayoutData(gdList);
		createCopyMenu(touchpointData);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.IUGroup_RequiredCapabilities);
		label.setLayoutData(gd2);
		requiredCapabilities = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		requiredCapabilities.setLayoutData(gdList);
		createCopyMenu(requiredCapabilities);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvAdminUIMessages.IUGroup_ProvidedCapabilities);
		label.setLayoutData(gd2);
		providedCapabilities = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		providedCapabilities.setLayoutData(gdList);
		createCopyMenu(providedCapabilities);

		if (editable) {
			id.addModifyListener(listener);
			version.addModifyListener(listener);
			namespace.addModifyListener(listener);
			touchpointType.addModifyListener(listener);
		} else {
			id.setEditable(false);
			version.setEditable(false);
			namespace.setEditable(false);
			touchpointType.setEditable(false);
		}
		initializeFields();
		return composite;
	}

	private void initializeFields() {
		IInstallableUnit iu = getIU();
		if (iu == null) {
			return;
		}
		id.setText(iu.getId());
		version.setText(iu.getVersion().toString());

		String value = iu.getProperty(IInstallableUnit.NAMESPACE_IU_ID);
		if (value != null) {
			namespace.setText(value);
		}
		ITouchpointType type = iu.getTouchpointType();
		if (type != null) {
			touchpointType.setText(type.getId());
		}
		Collection<ITouchpointData> data = iu.getTouchpointData();
		String[] items = new String[data.size()];
		int i = 0;
		for (ITouchpointData td : data) {
			items[i++] = td.toString();
		}
		touchpointData.setItems(items);

		Collection<IRequirement> reqs = iu.getRequirements();
		items = new String[reqs.size()];
		i = 0;
		for (IRequirement req : reqs) {
			items[i++] = req.toString();
		}
		requiredCapabilities.setItems(items);
		Collection<IProvidedCapability> prov = iu.getProvidedCapabilities();
		items = new String[prov.size()];
		i = 0;
		for (IProvidedCapability capability : prov) {
			items[i++] = capability.toString();
		}
		providedCapabilities.setItems(items);
	}

	public void updateIU() {
		// If it's not an InstallableUnit it is not editable
		if (iuElement == null || iuElement instanceof IInstallableUnit) {
			InstallableUnitDescription unit = new InstallableUnitDescription();
			unit.setId(id.getText().trim());
			unit.setVersion(Version.create(version.getText().trim()));
			unit.setProperty(IInstallableUnit.NAMESPACE_IU_ID, namespace.getText().trim());
			// TODO this is bogus because we don't let user provide a touchpoint type version
			unit.setTouchpointType(MetadataFactory.createTouchpointType(touchpointType.getText().trim(), Version.create("1.0.0"))); //$NON-NLS-1$
			iuElement = MetadataFactory.createInstallableUnit(unit);
		}
	}

	/**
	 * Return a status indicating the validity of the profile info
	 * 
	 * @return a status indicating the validity of the profile info
	 */
	public IStatus verify() {
		if (id.getText().trim().length() == 0) {
			return new Status(IStatus.ERROR, ProvAdminUIActivator.PLUGIN_ID, 0, ProvAdminUIMessages.IUGroup_IU_ID_Required, null);
		}

		// TODO what kind of validation do we perform for other properties?
		return new Status(IStatus.OK, ProvAdminUIActivator.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$

	}

	private void createCopyMenu(final List list) {
		Menu copyMenu = new Menu(list);
		MenuItem copyItem = new MenuItem(copyMenu, SWT.NONE);
		copyItem.addSelectionListener(new SelectionListener() {
			/*
			 * @see SelectionListener.widgetSelected (SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				copySelectionsToClipboard(list);
			}

			/*
			 * @see SelectionListener.widgetDefaultSelected(SelectionEvent)
			 */
			public void widgetDefaultSelected(SelectionEvent e) {
				copySelectionsToClipboard(list);
			}
		});
		copyItem.setText(JFaceResources.getString("copy")); //$NON-NLS-1$
		list.setMenu(copyMenu);
	}

	void copySelectionsToClipboard(List list) {
		StringBuffer buffer = new StringBuffer();
		String[] selections = list.getSelection();
		for (int i = 0; i < selections.length; i++) {
			buffer.append(selections[i]);
			buffer.append("\n"); //$NON-NLS-1$
		}
		Clipboard clipboard = new Clipboard(list.getDisplay());
		clipboard.setContents(new Object[] {buffer.toString()}, new Transfer[] {TextTransfer.getInstance()});
		clipboard.dispose();

	}

}
