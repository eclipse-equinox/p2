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
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import java.util.Map;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.ui.dialogs.RepositoryGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

/**
 * A RepositoryGroup is a reusable UI component that allows repository
 * attributes to be displayed and edited in different UI dialogs.
 * 
 * @since 3.4
 * 
 */
public class RepositoryImplementationGroup extends RepositoryGroup {

	Table propertiesTable;

	public RepositoryImplementationGroup(final Composite parent, IRepository repository, ModifyListener listener) {
		super(parent, repository, listener);
	}

	protected Composite createGroupComposite(final Composite parent, ModifyListener listener) {
		Composite comp = super.createGroupComposite(parent, listener);
		Label propertiesLabel = new Label(comp, SWT.NONE);
		propertiesLabel.setText(ProvAdminUIMessages.RepositoryGroup_PropertiesLabel);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		propertiesLabel.setLayoutData(data);

		propertiesTable = new Table(comp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		data.grabExcessVerticalSpace = true;
		propertiesTable.setLayoutData(data);
		propertiesTable.setHeaderVisible(true);
		TableColumn nameColumn = new TableColumn(propertiesTable, SWT.NONE);
		nameColumn.setText(ProvAdminUIMessages.RepositoryGroup_NameColumnLabel);
		TableColumn valueColumn = new TableColumn(propertiesTable, SWT.NONE);
		valueColumn.setText(ProvAdminUIMessages.RepositoryGroup_ValueColumnLabel);

		initializeTable();

		nameColumn.pack();
		valueColumn.pack();
		return comp;
	}

	private void initializeTable() {
		if (getRepository() != null) {
			Map repoProperties = getRepository().getProperties();
			if (repoProperties != null) {
				String[] propNames = (String[]) repoProperties.keySet().toArray(new String[repoProperties.size()]);
				for (int i = 0; i < propNames.length; i++) {
					TableItem item = new TableItem(propertiesTable, SWT.NULL);
					item.setText(new String[] {propNames[i], repoProperties.get(propNames[i]).toString()});
				}
			}
		}
	}
}
