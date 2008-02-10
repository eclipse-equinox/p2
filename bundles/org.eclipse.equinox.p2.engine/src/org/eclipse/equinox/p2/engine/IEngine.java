package org.eclipse.equinox.p2.engine;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public interface IEngine {
	public static final String SERVICE_NAME = IEngine.class.getName();

	public IStatus perform(IProfile profile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor);

}
