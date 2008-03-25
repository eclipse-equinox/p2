package org.eclipse.equinox.internal.p2.publisher.actions;

import java.util.Properties;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAdvice;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;

public interface IConfigAdvice extends IPublishingAdvice {

	public static final String ID = "config_advice"; //$NON-NLS-1$

	public BundleInfo[] getBundles();

	public Properties getProperties();
}
