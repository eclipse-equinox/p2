package org.eclipse.equinox.internal.provisional.p2.director;

import java.io.IOException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.engine.*;

public class PlanExecutionHelper {
	public static IStatus executePlan(ProvisioningPlan result, IEngine engine, ProvisioningContext context, IProgressMonitor progress) {
		if (!result.getStatus().isOK())
			return result.getStatus();

		if (result.getInstallerPlan() != null) {
			IStatus installerPlanStatus = engine.perform(result.getInstallerPlan().getProfileChangeRequest().getProfile(), new DefaultPhaseSet(), result.getInstallerPlan().getOperands(), context, progress);
			if (!installerPlanStatus.isOK())
				return installerPlanStatus;
			Configurator configChanger = (Configurator) ServiceHelper.getService(DirectorActivator.context, Configurator.class.getName());
			try {
				configChanger.applyConfiguration();
			} catch (IOException e) {
				return new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, "Unexpected failure applying configuration", e);
			}
		}
		return engine.perform(result.getProfileChangeRequest().getProfile(), new DefaultPhaseSet(), result.getOperands(), context, progress);
	}
}
