package org.eclipse.equinox.p2.operations;

import java.util.List;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;

/**
 * @since 2.8
 */
public interface IRemedy {

	RemedyConfig getConfig();

	List<RemedyIUDetail> getIusDetails();

	IProfileChangeRequest getRequest();

	int getBeingInstalledRelaxedWeight();

	int getInstallationRelaxedWeight();

}