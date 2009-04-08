package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

public interface ICapabilityAdvice extends IPublisherAdvice {

	public IProvidedCapability[] getProvidedCapabilities(InstallableUnitDescription iu);

	public IRequiredCapability[] getRequiredCapabilities(InstallableUnitDescription iu);

	public IRequiredCapability[] getMetaRequiredCapabilities(InstallableUnitDescription iu);
}
