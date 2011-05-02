/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

public interface IVersionAdvice extends IPublisherAdvice {

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

}
