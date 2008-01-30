/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.viewers;

import java.net.URL;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.model.ProfileElement;
import org.eclipse.equinox.p2.ui.model.RepositoryElement;
import org.eclipse.jface.viewers.IElementComparer;

public class ProvElementComparer implements IElementComparer {

	public boolean equals(Object a, Object b) {
		IInstallableUnit iu1 = getIU(a);
		IInstallableUnit iu2 = getIU(b);
		if (iu1 != null && iu2 != null)
			return iu1.equals(iu2);
		String p1 = getProfileId(a);
		String p2 = getProfileId(b);
		if (p1 != null && p2 != null)
			return p1.equals(p2);
		URL r1 = getRepositoryLocation(a);
		URL r2 = getRepositoryLocation(b);
		if (r1 != null && r2 != null)
			return r1.toExternalForm().equals(r2.toExternalForm());
		return a.equals(b);
	}

	public int hashCode(Object element) {
		IInstallableUnit iu = getIU(element);
		if (iu != null)
			return iu.hashCode();
		String profileId = getProfileId(element);
		if (profileId != null)
			return profileId.hashCode();
		URL url = getRepositoryLocation(element);
		if (url != null)
			return url.toExternalForm().hashCode();
		return element.hashCode();
	}

	private IInstallableUnit getIU(Object obj) {
		return (IInstallableUnit) ProvUI.getAdapter(obj, IInstallableUnit.class);
	}

	private String getProfileId(Object obj) {
		if (obj instanceof ProfileElement)
			return ((ProfileElement) obj).getLabel(obj);
		Profile profile = (Profile) ProvUI.getAdapter(obj, Profile.class);
		if (profile == null)
			return null;
		return profile.getProfileId();
	}

	private URL getRepositoryLocation(Object obj) {
		if (obj instanceof RepositoryElement)
			return ((RepositoryElement) obj).getLocation();
		return null;
	}

}
