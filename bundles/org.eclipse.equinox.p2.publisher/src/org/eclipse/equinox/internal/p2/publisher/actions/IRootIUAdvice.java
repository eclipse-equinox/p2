/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.util.Collection;

public interface IRootIUAdvice {

	/**
	 * Returns the list of children of the root for this publishing operation.
	 * Returned elements are either the String id of the IUs or the IUs themselves.
	 * @return the collection of children
	 */
	public Collection getChildren();
}