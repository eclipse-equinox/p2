/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.viewers;

/**
 * Data structure describing a column to be shown in an IU view.  
 * 
 * @since 3.4
 */
public class IUColumnConfig {
	public final static int COLUMN_ID = 0;
	public final static int COLUMN_NAME = 1;
	public final static int COLUMN_VERSION = 2;
	public final static int COLUMN_SIZE = 3;

	public String columnTitle;
	public int columnField;
	public int defaultColumnWidth;
	public int columnWidth;

	public IUColumnConfig(String title, int columnField, int defaultColumnWidth) {
		this.columnTitle = title;
		this.columnField = columnField;
		this.defaultColumnWidth = defaultColumnWidth;
		this.columnWidth = -1;
	}

	public int getWidth() {
		if (columnWidth >= 0)
			return columnWidth;
		return defaultColumnWidth;
	}
}
