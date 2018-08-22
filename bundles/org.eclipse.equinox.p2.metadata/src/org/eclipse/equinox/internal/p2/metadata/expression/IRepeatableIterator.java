/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.Iterator;

public interface IRepeatableIterator<T> extends Iterator<T> {
	/**
	 * Returns a copy that will iterate over the same elements
	 * as this iterator. The contents or position of this iterator
	 * is left unchanged. 
	 * @return A re-initialized copy of this iterator.
	 */
	IRepeatableIterator<T> getCopy();

	Object getIteratorProvider();
}
