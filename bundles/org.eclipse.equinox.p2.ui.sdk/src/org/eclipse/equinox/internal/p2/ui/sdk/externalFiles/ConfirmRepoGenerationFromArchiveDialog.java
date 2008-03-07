package org.eclipse.equinox.internal.p2.ui.sdk.externalFiles;

import java.io.File;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

/**
 * @since 3.4
 *
 */
public class ConfirmRepoGenerationFromArchiveDialog extends ConfirmWithPathDetailsDialog {

	public ConfirmRepoGenerationFromArchiveDialog(Shell parentShell, File archive, File targetRepoLocation) {
		super(parentShell, targetRepoLocation, NLS.bind(ProvSDKMessages.ConfirmRepoGenerationFromArchiveDialog_Message, archive.getAbsolutePath()), ProvSDKUIActivator.getDefault().getPreferenceStore(), PreferenceConstants.PREF_GENERATE_REPOFOLDER);
	}

	protected String getLocationDescription() {
		return ProvSDKMessages.ConfirmRepoGenerationFromArchiveDialog_FileDescription;
	}
}