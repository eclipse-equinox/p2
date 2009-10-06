/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;

/**
 * Collect a single element and stop the Query
 */
public class SingleElementCollector extends Collector {

	public boolean accept(Object object) {
		super.accept(object);
		return false;
	}

	public Object getElement() {
		if (!isEmpty())
			return iterator().next();
		return null;
	}
}
