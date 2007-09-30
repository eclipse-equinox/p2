/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.ui.ProvUIActivator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.osgi.framework.Version;

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

		// TODO will existing IUs be editable?
		boolean editable = iu == null && listener != null;

		Label label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.IUGroup_ID);
		id = new Text(composite, SWT.BORDER);
		id.setLayoutData(gd);
		if (editable) {
			id.addModifyListener(listener);
		} else {
			id.setEditable(false);
		}

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.IUGroup_Version);
		version = new Text(composite, SWT.BORDER);
		version.setLayoutData(gd);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.IUGroup_Namespace);
		namespace = new Text(composite, SWT.BORDER);
		namespace.setLayoutData(gd);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.IUGroup_TouchpointType);
		touchpointType = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		touchpointType.setLayoutData(gd);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.IUGroup_TouchpointData);
		label.setLayoutData(gd2);
		touchpointData = new List(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		touchpointData.setLayoutData(gdList);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.IUGroup_RequiredCapabilities);
		label.setLayoutData(gd2);
		requiredCapabilities = new List(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		requiredCapabilities.setLayoutData(gdList);

		label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.IUGroup_ProvidedCapabilities);
		label.setLayoutData(gd2);
		providedCapabilities = new List(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		providedCapabilities.setLayoutData(gdList);

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
		if (iu == null) {
			return;
		}
		id.setText(iu.getId());
		version.setText(iu.getVersion().toString());

		String value = iu.getProperty(IInstallableUnit.IU_NAMESPACE);
		if (value != null) {
			namespace.setText(value);
		}
		TouchpointType type = iu.getTouchpointType();
		if (type != null) {
			touchpointType.setText(type.getId());
		}
		TouchpointData[] data = iu.getTouchpointData();
		String[] items = new String[data.length];
		for (int i = 0; i < data.length; i++) {
			items[i] = data[i].toString();
		}
		touchpointData.setItems(items);

		RequiredCapability[] req = iu.getRequiredCapabilities();
		items = new String[req.length];
		for (int i = 0; i < req.length; i++) {
			items[i] = req[i].toString();
		}
		requiredCapabilities.setItems(items);
		ProvidedCapability[] prov = iu.getProvidedCapabilities();
		items = new String[prov.length];
		for (int i = 0; i < prov.length; i++) {
			items[i] = prov[i].toString();
		}
		providedCapabilities.setItems(items);
	}

	public void updateIU() {
		if (iu == null) {
			iu = new InstallableUnit();
		}
		// If it's not an InstallableUnit it is not editable
		if (iu instanceof InstallableUnit) {
			InstallableUnit unit = (InstallableUnit) iu;
			unit.setId(id.getText().trim());
			unit.setVersion(new Version(version.getText().trim()));
			unit.setProperty(IInstallableUnit.IU_NAMESPACE, namespace.getText().trim());
			// TODO this is bogus because we don't let user provide a touchpoint
			// type version
			unit.setTouchpointType(new TouchpointType(touchpointType.getText().trim(), new Version("1.0.0"))); //$NON-NLS-1$
		}
	}

	/**
	 * Return a status indicating the validity of the profile info
	 * 
	 * @return a status indicating the validity of the profile info
	 */
	public IStatus verify() {
		if (id.getText().trim().length() == 0) {
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.IUGroup_IU_ID_Required, null);
		}

		// TODO what kind of validation do we perform for other properties?
		return new Status(IStatus.OK, ProvUIActivator.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$

	}
}
