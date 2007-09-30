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

package org.eclipse.equinox.prov.ui.viewers;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.IInstallableUnitConstants;
import org.eclipse.equinox.prov.ui.ProvUIImages;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for showing IU's in a table.
 * 
 * @since 3.4
 */
public class IUDetailsLabelProvider extends LabelProvider implements ITableLabelProvider {
	final static int COLUMN_NAME = 0;
	final static int COLUMN_VERSION = 1;
	final static String BLANK = ""; //$NON-NLS-1$

	public String getText(Object obj) {
		return getColumnText(obj, COLUMN_NAME);
	}

	public Image getImage(Object obj) {
		return getColumnImage(obj, COLUMN_NAME);
	}

	public String getColumnText(Object element, int columnIndex) {
		IInstallableUnit iu = getIU(element);
		if (iu == null)
			return BLANK;

		switch (columnIndex) {
			case COLUMN_NAME :
				String name = iu.getProperty(IInstallableUnitConstants.NAME);
				if (name != null)
					return name;
				return iu.getId();
			case COLUMN_VERSION :
				return iu.getVersion().toString();
		}
		return BLANK;
	}

	public Image getColumnImage(Object element, int index) {
		if (index == COLUMN_NAME && getIU(element) != null) {
			return ProvUIImages.getImage(ProvUIImages.IMG_IU);
		}
		return null;
	}

	private IInstallableUnit getIU(Object element) {
		if (element instanceof IInstallableUnit)
			return (IInstallableUnit) element;
		if (element instanceof IAdaptable)
			return (IInstallableUnit) ((IAdaptable) element).getAdapter(IInstallableUnit.class);
		return null;
	}
}