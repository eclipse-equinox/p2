package org.eclipse.equinox.internal.provisional.p2.ui;

import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;

/**
 * Help context ids for the P2 UI
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 * @since 3.4
 */

public interface IProvHelpContextIds {
	public static final String PREFIX = ProvUIActivator.PLUGIN_ID + "."; //$NON-NLS-1$

	public static final String REVERT_CONFIGURATION_WIZARD = PREFIX + "revert_configuration_wizard_context"; //$NON-NLS-1$ 

	public static final String UNINSTALL_WIZARD = PREFIX + "uinstall_wizard_context"; //$NON-NLS-1$ 

	public static final String UPDATE_WIZARD = PREFIX + "update_wizard_context"; //$NON-NLS-1$ 

	public static final String ADD_REPOSITORY_DIALOG = PREFIX + "add_repository_dialog_context"; //$NON-NLS-1$ 

	public static final String INSTALL_WIZARD = PREFIX + "install_wizard_context"; //$NON-NLS-1$ 

}
