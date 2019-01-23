package org.eclipse.equinox.p2.examples.rcp.prestartupdate;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private static final String JUSTUPDATED = "justUpdated";

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(600, 400));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(false);
	}

	@Override
	public void postWindowOpen() {
		final IProvisioningAgent agent = (IProvisioningAgent) ServiceHelper.getService(Activator.bundleContext,
				IProvisioningAgent.SERVICE_NAME);
		if (agent == null) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"No provisioning agent found.  This application is not set up for updates."));
		}
		// XXX if we're restarting after updating, don't check again.
		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
		if (prefStore.getBoolean(JUSTUPDATED)) {
			prefStore.setValue(JUSTUPDATED, false);
			return;
		}

		// XXX check for updates before starting up.
		// If an update is performed, restart. Otherwise log
		// the status.
		IRunnableWithProgress runnable = monitor -> {
			IStatus updateStatus = P2Util.checkForUpdates(agent, monitor);
			if (updateStatus.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
				PlatformUI.getWorkbench().getDisplay()
						.asyncExec(() -> MessageDialog.openInformation(null, "Updates", "No updates were found"));
			} else if (updateStatus.getSeverity() != IStatus.ERROR) {
				prefStore.setValue(JUSTUPDATED, true);
				PlatformUI.getWorkbench().restart();
			} else {
				LogHelper.log(updateStatus);
			}
		};
		try {
			new ProgressMonitorDialog(null).run(true, true, runnable);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
		}

	}

}
