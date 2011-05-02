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