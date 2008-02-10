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
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.RepositoryPropertyPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

/**
 * PropertyPage that shows a repository's properties
 * 
 * @since 3.4
 */
public class RepositoryImplementationPropertyPage extends RepositoryPropertyPage {

	private Table propertiesTable;

	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		if (control instanceof Composite) {
			Composite comp = (Composite) control;
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
		}
		return control;

	}

	private void initializeTable() {
		if (getRepositoryElement() != null) {
			Map repoProperties = getRepositoryElement().getRepository(null).getProperties();
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
