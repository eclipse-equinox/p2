/*******************************************************************************
 *  Copyright (c) 2007, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.viewers;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;

/**
 * An IUColumnConfig describes a column in a viewer that is showing 
 * information about an IIInstallableUnit.  
 * 
 * @since 3.4
 */
public class IUColumnConfig {
	/**
	 * Constant indicating that the column represents an IInstallableUnit's id
	 */
	public final static int COLUMN_ID = 0;

	/**
	 * Constant indicating that the column represents an IInstallableUnit's name
	 */
	public final static int COLUMN_NAME = 1;

	/**
	 * Constant indicating that the column represents an IInstallableUnit's version
	 */
	public final static int COLUMN_VERSION = 2;

	/**
	 * Constant indicating that the column represents an IInstallableUnit's size
	 */
	public final static int COLUMN_SIZE = 3;

	/**
	 * Constant indicating that the column represents an IInstallableUnit's provider
	 */
	public final static int COLUMN_PROVIDER = 4;

	/**
	 * Constant indicating that the column represents an IInstallableUnit's description
	 */
	public final static int COLUMN_DESCRIPTION = 5;

	private String columnTitle;
	private int columnType;
	private int columnWidth;
	private int columnWidthInPixels;

	/**
	 * Create an IUColumnConfig describing a column with the specified title, type, and
	 * default width.
	 * 
	 * @param title the title that should appear for the column
	 * @param columnType the type of column represented.  The type may be used to determine
	 * appropriate size or formatting of the column's content.
	 * @param columnWidthInChars the width (in characters) that should be used for the column if no 
	 * other width is specified by the client.
	 */

	public IUColumnConfig(String title, int columnType, int columnWidthInChars) {
		this.columnTitle = title;
		this.columnType = columnType;
		this.columnWidth = columnWidthInChars;
		this.columnWidthInPixels = -1;
	}

	/**
	 * Return the title of the column.
	 * @return the title that should be used for the column.
	 * 
	 * @since 3.6
	 */
	public String getColumnTitle() {
		return columnTitle;
	}

	/**
	 * Set the title of the column
	 * @param title the String that should be used when the column's title is shown
	 * 
	 * @since 3.6
	 */
	public void setColumnTitle(String title) {
		this.columnTitle = title;
	}

	/**
	 * Return the width of the column in character width units
	 * @return the width (in characters) of the column
	 * 
	 * @since 3.6
	 */
	public int getWidthInChars() {
		return columnWidth;
	}

	/**
	 * Set the width of the column in character width units
	 * @param columnWidth the width (in characters) of the column
	 * 
	 * @since 3.6
	 */
	public void setWidthInChars(int columnWidth) {
		this.columnWidth = columnWidth;
	}

	/**
	 * Get the width in pixels of this column when displayed in the specified
	 * control.  If a specific width in pixels has already been specified by a client,
	 * that width is used.  Otherwise, the value is computed based on the character
	 * width specified for the column.
	 * @param control
	 * @return the width in pixels that should be used for the column
	 * 
	 * @since 3.6
	 * 
	 * @see #setWidthInPixels(int)
	 */
	public int getWidthInPixels(Control control) {
		if (columnWidthInPixels >= 0)
			return columnWidthInPixels;

		GC gc = new GC(control);
		FontMetrics fm = gc.getFontMetrics();
		columnWidthInPixels = Dialog.convertWidthInCharsToPixels(fm, columnWidth);
		gc.dispose();
		return columnWidthInPixels;
	}

	/**
	 * Set the width in pixels that should be used for this column.  This width overrides
	 * any character width that has been specified.  This method is useful when the column width
	 * is determined by user action.
	 * 
	 * @param width
	 * 
	 * @since 3.6
	 */
	public void setWidthInPixels(int width) {
		this.columnWidthInPixels = width;
	}

	/**
	 * Return the type of column.
	 * @return an Integer constant specifying the type of data being shown in the column.
	 * 
	 * @since 3.6
	 * @see #COLUMN_ID
	 * @see #COLUMN_NAME
	 * @see #COLUMN_SIZE
	 * @see #COLUMN_VERSION
	 */
	public int getColumnType() {
		return columnType;
	}

	/**
	 * Set the type of column.
	 * @param type an Integer constant specifying the type of data being shown in the column.
	 * 
	 * @since 3.6
	 * 
	 * @see #COLUMN_ID
	 * @see #COLUMN_NAME
	 * @see #COLUMN_SIZE
	 * @see #COLUMN_VERSION
	 */
	public void setColumnType(int type) {
		this.columnType = type;
	}
}
