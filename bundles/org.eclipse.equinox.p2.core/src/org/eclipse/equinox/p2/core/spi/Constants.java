/*******************************************************************************
 *  Copyright (c)2012 Pascal Rapicault and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Pascal Rapicault - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core.spi;

public interface Constants {
	/*
	 * This constant is used internally in p2 to represent the case of a bundled macos application (the case where all the files are contained in the .app folder.
	 * It is typically used as an environment property in a profile. 
	 */
	public final String MACOSX_BUNDLED = "macosx-bundled"; //$NON-NLS-1$
}
