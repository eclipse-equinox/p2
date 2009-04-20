package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;

public class DirectorUtil {

	public static IStatus validateProfile(IProfile profile) {
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		ProvisioningContext ctx = new ProvisioningContext(new URI[0]);
		IPlanner planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.class.getName());
		return planner.getProvisioningPlan(pcr, ctx, null).getStatus();
	}
}
