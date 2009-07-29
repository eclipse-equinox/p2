/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

/**
 * ILayoutConstants defines common dialog constants used when laying
 * out dialogs.  Units of measurement are character widths and heights
 * unless otherwise specified.
 * 
 * This interface is not intended to be implemented
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.5
 */
public interface ILayoutConstants {
	public static final int DEFAULT_DESCRIPTION_HEIGHT = 4;
	public static final int MINIMUM_DESCRIPTION_HEIGHT = 1;
	public static final int DEFAULT_SITEDETAILS_HEIGHT = 2;
	public static final int DEFAULT_PRIMARY_COLUMN_WIDTH = 60;
	public static final int DEFAULT_COLUMN_WIDTH = 40;
	public static final int DEFAULT_SMALL_COLUMN_WIDTH = 20;
	public static final int DEFAULT_TABLE_HEIGHT = 10;
	public static final int DEFAULT_TABLE_WIDTH = 80;
	public static final int[] IUS_TO_DETAILS_WEIGHTS = new int[] {80, 20};
}
