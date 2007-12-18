/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *  IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.sar;

/**
 * Sar constants
 */
public interface SarConstants {

	/** <code>SARFILE_MARKER</code> */
	String SARFILE_MARKER = "SarFile"; //$NON-NLS-1$

	/** <code>SARFILE_VERSION</code> */
	int SARFILE_VERSION = 2;

	/**
	 * Comment for <code>DEFAULT_ENCODING</code>
	 */
	String DEFAULT_ENCODING = "UTF-8"; //$NON-NLS-1$

	/**
	 * Debug
	 */
	boolean DEBUG = false;
}
