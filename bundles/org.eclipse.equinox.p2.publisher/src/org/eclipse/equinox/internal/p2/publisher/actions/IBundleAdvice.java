package org.eclipse.equinox.internal.p2.publisher.actions;

import org.eclipse.equinox.internal.p2.publisher.BundleDescriptionFactory;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAdvice;

public interface IBundleAdvice extends IPublishingAdvice {

	public static final String ID = "bundle_advice";
	public static final String DIR = BundleDescriptionFactory.DIR;
	public static final String JAR = BundleDescriptionFactory.JAR;

	/**
	 * Returns the shape (e.g., folder or JAR) of the bundle with the given name and version.
	 * If the version is <code>null</code> then return the advice for the most likely version
	 * of the bundle.
	 * @param id the bundle to lookup
	 * @param version the version of the bundle (may be <code>null</code>)
	 * @return the shape of the given bundle.
	 */
	public String getShape(String id, String version);

	public void setShape(String id, String version, String shape);

}
