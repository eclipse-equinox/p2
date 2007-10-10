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

package org.eclipse.equinox.p2.ui.viewers;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitConstants;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.ui.model.AvailableIUElement;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for showing IU's in a table.  Clients can configure
 * what is shown in each column.
 * 
 * @since 3.4
 */
public class IUDetailsLabelProvider extends LabelProvider implements ITableLabelProvider {
	public final static int COLUMN_ID = 0;
	public final static int COLUMN_NAME = 1;
	public final static int COLUMN_VERSION = 2;
	public final static int COLUMN_SIZE = 3;

	final static int PRIMARY_COLUMN = 0;
	final static String BLANK = ""; //$NON-NLS-1$

	private int[] columnConfig = {COLUMN_ID, COLUMN_VERSION};

	public IUDetailsLabelProvider() {
		// use default column config
	}

	public IUDetailsLabelProvider(int[] columnConfig) {
		Assert.isLegal(columnConfig.length > 0);
		this.columnConfig = columnConfig;
	}

	public String getText(Object obj) {
		return getColumnText(obj, columnConfig[0]);
	}

	public Image getImage(Object obj) {
		return getColumnImage(obj, columnConfig[0]);
	}

	public String getColumnText(Object element, int columnIndex) {
		int columnContent = COLUMN_ID;
		if (columnIndex <= columnConfig.length) {
			columnContent = columnConfig[columnIndex];
		}

		IInstallableUnit iu = getIU(element);
		if (iu == null)
			return BLANK;

		switch (columnContent) {
			case COLUMN_ID :
				return iu.getId();
			case COLUMN_NAME :
				String name = iu.getProperty(IInstallableUnitConstants.NAME);
				if (name != null)
					return name;
				return BLANK;
			case COLUMN_VERSION :
				return iu.getVersion().toString();
			case COLUMN_SIZE :
				return getIUSize(element);
		}
		return BLANK;
	}

	public Image getColumnImage(Object element, int index) {
		if (index == PRIMARY_COLUMN && getIU(element) != null) {
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

	private String getIUSize(Object element) {
		if (element instanceof AvailableIUElement)
			return Integer.toString(((AvailableIUElement) element).getSize());
		return ProvUIMessages.IUDetailsLabelProvider_Unknown;
	}
}