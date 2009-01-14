package org.eclipse.equinox.internal.provisional.p2.engine;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.osgi.util.NLS;

public class MissingAction extends ProvisioningAction {

	private String actionId;
	private VersionRange versionRange;

	public MissingAction(String actionId, VersionRange versionRange) {
		this.actionId = actionId;
		this.versionRange = versionRange;
	}

	public String getActionId() {
		return actionId;
	}

	public VersionRange getVersionRange() {
		return versionRange;
	}

	public IStatus execute(Map parameters) {
		throw new IllegalArgumentException(NLS.bind(Messages.action_not_found, actionId + (versionRange == null ? "" : "/" + versionRange.toString()))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IStatus undo(Map parameters) {
		// do nothing as we want this action to undo successfully
		return Status.OK_STATUS;
	}
}
