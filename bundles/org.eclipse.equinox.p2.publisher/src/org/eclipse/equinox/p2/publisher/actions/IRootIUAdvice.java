/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others.
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
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.util.Collection;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherResult;

public interface IRootIUAdvice extends IPublisherAdvice {

	/**
	 * Returns the list of children of the root for this publishing operation.
	 * Returned elements are either the String id of the IUs, a VersionedName describing
	 * the IU or the IUs themselves.
	 * @param result
	 * @return the collection of children
	 */
	public Collection<? extends Object> getChildren(IPublisherResult result);
}