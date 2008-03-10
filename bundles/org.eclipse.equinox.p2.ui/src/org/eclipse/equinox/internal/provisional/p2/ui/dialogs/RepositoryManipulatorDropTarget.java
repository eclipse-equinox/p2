package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @since 3.4
 *
 */
public class RepositoryManipulatorDropTarget extends URLDropAdapter {
	IRepositoryManipulator manipulator;
	Control control;

	public RepositoryManipulatorDropTarget(IRepositoryManipulator manipulator, Control control) {
		Assert.isNotNull(manipulator);
		this.manipulator = manipulator;
		this.control = control;
	}

	protected void handleURLString(String urlText, final DropTargetEvent event) {
		event.detail = DND.DROP_NONE;
		if (!dropTargetIsValid(event))
			return;
		final URL url;
		try {
			url = new URL(urlText);
		} catch (MalformedURLException e) {
			ProvUI.reportStatus(URLValidator.getInvalidURLStatus(urlText), StatusManager.SHOW | StatusManager.LOG);
			return;
		}
		BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
			public void run() {
				IStatus status = manipulator.getURLValidator(control.getShell()).validateRepositoryURL(url, true, null);
				if (status.isOK()) {
					ProvisioningOperation addOperation = manipulator.getAddOperation(url);
					ProvisioningOperationRunner.run(addOperation, control.getShell());
					event.detail = DND.DROP_LINK;
				} else if (status.getCode() == URLValidator.REPO_AUTO_GENERATED) {
					event.detail = DND.DROP_COPY;
				} else if (!(status.getCode() == URLValidator.ALTERNATE_ACTION_TAKEN)) {
					ProvUI.reportStatus(status, StatusManager.BLOCK | StatusManager.LOG);
				}

			}
		});
	}
}