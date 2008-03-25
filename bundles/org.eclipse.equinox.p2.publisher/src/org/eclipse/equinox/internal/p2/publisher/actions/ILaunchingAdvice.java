package org.eclipse.equinox.internal.p2.publisher.actions;

import org.eclipse.equinox.internal.p2.publisher.IPublishingAdvice;

public interface ILaunchingAdvice extends IPublishingAdvice {

	public String[] getVMArguments();

	public String[] getProgramArguments();

	public String getExecutableName();
}
