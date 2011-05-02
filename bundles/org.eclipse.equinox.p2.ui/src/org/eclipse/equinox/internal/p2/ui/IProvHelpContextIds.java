/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;


/**
 * Help context ids for the P2 UI
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 * @since 3.4
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */

public interface IProvHelpContextIds {
	public static final String PREFIX = ProvUIActivator.PLUGIN_ID + "."; //$NON-NLS-1$

	public static final String REVERT_CONFIGURATION_WIZARD = PREFIX + "revert_configuration_wizard_context"; //$NON-NLS-1$ 

	public static final String UNINSTALL_WIZARD = PREFIX + "uinstall_wizard_context"; //$NON-NLS-1$ 

	public static final String UPDATE_WIZARD = PREFIX + "update_wizard_context"; //$NON-NLS-1$ 

	public static final String ADD_REPOSITORY_DIALOG = PREFIX + "add_repository_dialog_context"; //$NON-NLS-1$ 

	public static final String INSTALL_WIZARD = PREFIX + "install_wizard_context"; //$NON-NLS-1$ 

	public static final String REPOSITORY_MANIPULATION_DIALOG = PREFIX + "repository_manipulation_dialog_context"; //$NON-NLS-1$

	public static final String INSTALLED_SOFTWARE = PREFIX + "installed_software_context"; //$NON-NLS-1$

	public static final String AVAILABLE_SOFTWARE = PREFIX + "available_software_context"; //$NON-NLS-1$

}
