/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.core;

/**
 * An extension to {@link IServiceUI} for prompting the user about installing unsigned content.
 * @TODO This should be merged into IServiceUI during API cleanup
 */
public interface IServiceUICheckUnsigned {
	/**
	 * Prompts the user that they are installing unsigned content.
	 * @param details Detailed information about the items that have unsigned content.
	 * @return <code>true</code> if the installation should proceed, and <code>false</code> otherwise.
	 */
	public boolean promptForUnsignedContent(String[] details);

}
