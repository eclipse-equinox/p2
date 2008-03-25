package org.eclipse.equinox.internal.p2.publisher.actions;

import org.eclipse.equinox.internal.p2.publisher.BundleDescriptionFactory;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAdvice;

public interface IBundleShapeAdvice extends IPublishingAdvice {
	public static final String DIR = BundleDescriptionFactory.DIR;
	public static final String JAR = BundleDescriptionFactory.JAR;

	public String getShape();
}
