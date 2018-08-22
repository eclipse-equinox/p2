/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 * All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;

public class BundleShapeAdvice extends AbstractAdvice implements IBundleShapeAdvice {

	private final String shape;
	private final Version version;
	private final String id;

	public BundleShapeAdvice(String id, Version version, String shape) {
		this.id = id;
		this.version = version;
		this.shape = shape;
	}

	@Override
	protected String getId() {
		return id;
	}

	@Override
	protected Version getVersion() {
		return version;
	}

	@Override
	public String getShape() {
		return shape;
	}

}
