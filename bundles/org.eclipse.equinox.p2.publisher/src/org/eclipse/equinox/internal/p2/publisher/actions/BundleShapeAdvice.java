package org.eclipse.equinox.internal.p2.publisher.actions;

import org.osgi.framework.Version;

public class BundleShapeAdvice extends AbstractAdvice implements IBundleShapeAdvice {

	private String shape;
	private Version version;
	private String id;

	public BundleShapeAdvice(String id, Version version, String shape) {
		this.id = id;
		this.version = version;
		this.shape = shape;
	}

	protected String getId() {
		return id;
	}

	protected Version getVersion() {
		return version;
	}

	public String getShape() {
		return shape;
	}

}
