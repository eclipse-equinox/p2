/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
