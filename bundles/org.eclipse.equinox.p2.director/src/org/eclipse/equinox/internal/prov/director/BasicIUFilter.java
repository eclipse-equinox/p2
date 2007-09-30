/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.prov.director;

import org.eclipse.equinox.prov.metadata.IInstallableUnit;

public class BasicIUFilter extends IUFilter {
	private IInstallableUnit[] accepted;

	public BasicIUFilter(IInstallableUnit[] accepted) {
		this.accepted = accepted;
	}

	public boolean accept(IInstallableUnit iu) {
		for (int i = 0; i < accepted.length; i++) {
			if (accepted[i].equals(iu))
				return true;
		}
		return false;
	}

}
