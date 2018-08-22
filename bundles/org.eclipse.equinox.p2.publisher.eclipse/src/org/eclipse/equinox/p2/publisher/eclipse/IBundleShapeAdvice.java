/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 and others.
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

import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

public interface IBundleShapeAdvice extends IPublisherAdvice {
	public static final String DIR = BundlesAction.DIR;
	public static final String JAR = BundlesAction.JAR;

	public String getShape();
}
