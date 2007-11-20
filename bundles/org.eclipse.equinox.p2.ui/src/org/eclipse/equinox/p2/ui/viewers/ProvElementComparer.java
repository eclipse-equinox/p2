/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.viewers;

import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.jface.viewers.IElementComparer;

public class ProvElementComparer implements IElementComparer {

	public boolean equals(Object a, Object b) {
		IInstallableUnit iu1 = getIU(a);
		IInstallableUnit iu2 = getIU(b);
		if (iu1 != null && iu2 != null)
			return iu1.equals(iu2);
		Profile p1 = getProfile(a);
		Profile p2 = getProfile(b);
		if (p1 != null && p2 != null)
			return p1.equals(p2);
		IRepository r1 = getRepository(a);
		IRepository r2 = getRepository(b);
		if (r1 != null && r2 != null)
			return r1.equals(r2);
		return a.equals(b);
	}

	public int hashCode(Object element) {
		IInstallableUnit iu = getIU(element);
		if (iu != null)
			return iu.hashCode();
		Profile profile = getProfile(element);
		if (profile != null)
			return profile.hashCode();
		IRepository repo = getRepository(element);
		if (repo != null)
			return repo.hashCode();
		return element.hashCode();
	}

	private IInstallableUnit getIU(Object obj) {
		return (IInstallableUnit) ProvUI.getAdapter(obj, IInstallableUnit.class);
	}

	private Profile getProfile(Object obj) {
		return (Profile) ProvUI.getAdapter(obj, Profile.class);
	}

	private IRepository getRepository(Object obj) {
		return (IRepository) ProvUI.getAdapter(obj, IRepository.class);
	}

}
