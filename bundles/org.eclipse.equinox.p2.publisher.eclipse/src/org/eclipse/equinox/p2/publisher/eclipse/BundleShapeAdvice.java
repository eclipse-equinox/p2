/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
