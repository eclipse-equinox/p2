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
package org.eclipse.equinox.prov.ui.dialogs;

import java.util.Map;
import org.eclipse.equinox.prov.core.repository.IRepositoryInfo;
import org.eclipse.equinox.prov.ui.internal.ProvUIMessages;
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

	public RepositoryImplementationGroup(final Composite parent, IRepositoryInfo repository, ModifyListener listener, final boolean chooseFile, final String dirPath, final String fileName) {
		super(parent, repository, listener, chooseFile, dirPath, fileName);
	}

	protected Composite createGroupComposite(final Composite parent, ModifyListener listener, final boolean chooseFile, final String dirPath, final String fileName) {
		Composite comp = super.createGroupComposite(parent, listener, chooseFile, dirPath, fileName);
		Label propertiesLabel = new Label(comp, SWT.NONE);
		propertiesLabel.setText(ProvUIMessages.RepositoryGroup_PropertiesLabel);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		propertiesLabel.setLayoutData(data);

		propertiesTable = new Table(comp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		data.grabExcessVerticalSpace = true;
		propertiesTable.setLayoutData(data);
		propertiesTable.setHeaderVisible(true);
		TableColumn nameColumn = new TableColumn(propertiesTable, SWT.NONE);
		nameColumn.setText(ProvUIMessages.RepositoryGroup_NameColumnLabel);
		TableColumn valueColumn = new TableColumn(propertiesTable, SWT.NONE);
		valueColumn.setText(ProvUIMessages.RepositoryGroup_ValueColumnLabel);

		initializeTable();

		nameColumn.pack();
		valueColumn.pack();
		return comp;
	}

	private void initializeTable() {
		if (repository != null) {
			Map repoProperties = repository.getProperties();
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
