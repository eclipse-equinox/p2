package org.eclipse.equinox.internal.p2.publisher;

import org.osgi.framework.Version;

public interface IPublishingAdvice {

	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version);

}
