package org.eclipse.equinox.internal.p2.publisher.actions;

import org.eclipse.equinox.internal.p2.publisher.IPublishingAdvice;
import org.osgi.framework.Version;

public interface IVersionAdvice extends IPublishingAdvice {

	public static final String ID = "version_advice"; //$NON-NLS-1$
	public static final String NS_BUNDLE = "bundle"; //$NON-NLS-1$
	public static final String NS_FEATURE = "feature"; //$NON-NLS-1$
	public static final String NS_ROOT = "root"; //$NON-NLS-1$

	/**
	 * Returns the version advice for the given id in the given namespace.
	 * @param namespace the namespace in which to look for advice
	 * @param id the item for which advice is sought
	 * @return the version advice found or <code>null</code> if none
	 */
	public Version getVersion(String namespace, String id);

	/**
	 * Sets the version advice for the given id in the given namespace.
	 * @param namespace the namespace in which to look for advice
	 * @param id the item for which advice is sought
	 * @param version the version advice for the given id or <code>null</code> to remove advice
	 */
	public void setVersion(String namespace, String id, Version version);

}
