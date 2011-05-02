/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * An IUPropertiesGroup is a reusable UI component that displays and edits the 
 * user-oriented properties of an IU. It can be used in 
 * different dialogs that manipulate or define IU's.
 * 
 * @since 3.4
 */
public class IUPropertiesGroup extends IUGroup {

	private Table propertiesTable;

	public IUPropertiesGroup(final Composite parent, IInstallableUnit iu, ModifyListener listener) {
		super(parent, iu, listener);
	}

	protected Composite createGroupComposite(Composite parent, ModifyListener listener) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.widthHint = 400;
		composite.setLayoutData(data);

		propertiesTable = new Table(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		propertiesTable.setLayoutData(data);
		propertiesTable.setHeaderVisible(true);
		TableColumn nameColumn = new TableColumn(propertiesTable, SWT.NONE);
		nameColumn.setResizable(true);
		nameColumn.setWidth(200);
		TableColumn valueColumn = new TableColumn(propertiesTable, SWT.NONE);
		valueColumn.setResizable(true);
		valueColumn.setWidth(250);
		initializeFields();
		return composite;
	}

	private void initializeFields() {
		if (iuElement == null) {
			return;
		}
		String[] propNames = new String[] {IInstallableUnit.PROP_NAME, IInstallableUnit.PROP_DESCRIPTION, IInstallableUnit.PROP_PROVIDER};
		String[] userPropNames = new String[] {ProvAdminUIMessages.IUPropertiesGroup_NamePropertyLabel, ProvAdminUIMessages.IUPropertiesGroup_DescriptionPropertyLabel, ProvAdminUIMessages.IUPropertiesGroup_ProviderPropertyLabel};
		for (int i = 0; i < propNames.length; i++) {
			TableItem item = new TableItem(propertiesTable, SWT.NULL);
			String value = getIU().getProperty(propNames[i], null);
			if (value != null)
				item.setText(new String[] {userPropNames[i], value});
		}
	}
}
