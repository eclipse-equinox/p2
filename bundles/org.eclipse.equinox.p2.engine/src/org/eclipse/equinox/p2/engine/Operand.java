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
package org.eclipse.equinox.p2.engine;

import org.eclipse.equinox.p2.metadata.IResolvedInstallableUnit;

public class Operand {
	private final IResolvedInstallableUnit first;
	private final IResolvedInstallableUnit second;

	public Operand(IResolvedInstallableUnit first, IResolvedInstallableUnit second) {
		this.first = first;
		this.second = second;
	}

	public IResolvedInstallableUnit first() {
		return first;
	}

	public IResolvedInstallableUnit second() {
		return second;
	}

	public String toString() {
		return first + " --> " + second; //$NON-NLS-1$
	}
}
