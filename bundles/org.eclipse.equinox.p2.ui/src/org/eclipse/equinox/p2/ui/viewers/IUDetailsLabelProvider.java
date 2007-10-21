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

import java.text.NumberFormat;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitConstants;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.ui.model.AvailableIUElement;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for showing IU's in a table.  Clients can configure
 * what is shown in each column.
 * 
 * @since 3.4
 */
public class IUDetailsLabelProvider extends ColumnLabelProvider implements ITableLabelProvider {
	final static int PRIMARY_COLUMN = 0;
	final static String BLANK = ""; //$NON-NLS-1$
	private String toolTipProperty = null;

	private IUColumnConfig[] columnConfig = ProvUI.getIUColumnConfig();

	public IUDetailsLabelProvider() {
		// use default column config
	}

	public IUDetailsLabelProvider(IUColumnConfig[] columnConfig) {
		Assert.isLegal(columnConfig.length > 0);
		this.columnConfig = columnConfig;
	}

	public String getText(Object obj) {
		return getColumnText(obj, columnConfig[0].columnField);
	}

	public Image getImage(Object obj) {
		return getColumnImage(obj, columnConfig[0].columnField);
	}

	public String getColumnText(Object element, int columnIndex) {
		int columnContent = IUColumnConfig.COLUMN_ID;
		if (columnIndex < columnConfig.length) {
			columnContent = columnConfig[columnIndex].columnField;
		}

		IInstallableUnit iu = getIU(element);
		if (iu == null)
			return BLANK;

		switch (columnContent) {
			case IUColumnConfig.COLUMN_ID :
				return iu.getId();
			case IUColumnConfig.COLUMN_NAME :
				String name = iu.getProperty(IInstallableUnitConstants.NAME);
				if (name != null)
					return name;
				return BLANK;
			case IUColumnConfig.COLUMN_VERSION :
				return iu.getVersion().toString();
			case IUColumnConfig.COLUMN_SIZE :
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
		if (element instanceof AvailableIUElement) {
			long size = ((AvailableIUElement) element).getSize();
			if (size != AvailableIUElement.SIZE_UNKNOWN) {
				return getFormattedSize(size);
			}
		}
		return ProvUIMessages.IUDetailsLabelProvider_Unknown;
	}

	private String getFormattedSize(long size) {
		if (size > 1000L) {
			long kb = size / 1000L;
			return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_KB, NumberFormat.getInstance().format(new Long(kb)));
		}
		return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_Bytes, NumberFormat.getInstance().format(new Long(size)));
	}

	public void setToolTipProperty(String propertyName) {
		toolTipProperty = propertyName;
	}

	public String getToolTipText(Object element) {
		IInstallableUnit iu = getIU(element);
		if (iu == null || toolTipProperty == null)
			return null;
		return iu.getProperty(toolTipProperty);
	}
}
