/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import org.eclipse.equinox.internal.provisional.p2.query.Collector;

/**
 * A collector that short-circuits on the first match.
 */
public class HasMatchCollector extends Collector {
	private boolean hasMatch = false;

	public boolean accept(Object object) {
		hasMatch = true;
		return false;
	}

	public boolean isEmpty() {
		return !hasMatch;
	}

}
