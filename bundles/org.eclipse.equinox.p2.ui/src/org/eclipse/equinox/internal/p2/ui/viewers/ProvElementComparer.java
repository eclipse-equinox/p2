/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.viewers;

import java.net.URI;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.IElementComparer;

public class ProvElementComparer implements IElementComparer {

	public boolean equals(Object a, Object b) {
		// We treat category elements specially because this
		// is one case where resolving down to an IU will lose identity
		// differences.  (category IU's with the same name and version number cannot be treated the same).
		if (a instanceof CategoryElement || b instanceof CategoryElement)
			return a.equals(b);
		IInstallableUnit iu1 = getIU(a);
		IInstallableUnit iu2 = getIU(b);
		if (iu1 != null && iu2 != null)
			return iu1.equals(iu2);
		String p1 = getProfileId(a);
		String p2 = getProfileId(b);
		if (p1 != null && p2 != null)
			return p1.equals(p2);
		URI r1 = getRepositoryLocation(a);
		URI r2 = getRepositoryLocation(b);
		if (r1 != null && r2 != null)
			return r1.equals(r2);
		return a.equals(b);
	}

	public int hashCode(Object element) {
		if (element instanceof CategoryElement)
			return element.hashCode();
		IInstallableUnit iu = getIU(element);
		if (iu != null)
			return iu.hashCode();
		String profileId = getProfileId(element);
		if (profileId != null)
			return profileId.hashCode();
		URI location = getRepositoryLocation(element);
		if (location != null)
			return location.hashCode();
		return element.hashCode();
	}

	private IInstallableUnit getIU(Object obj) {
		return ProvUI.getAdapter(obj, IInstallableUnit.class);
	}

	private String getProfileId(Object obj) {
		if (obj instanceof ProfileElement)
			return ((ProfileElement) obj).getLabel(obj);
		IProfile profile = ProvUI.getAdapter(obj, IProfile.class);
		if (profile == null)
			return null;
		return profile.getProfileId();
	}

	private URI getRepositoryLocation(Object obj) {
		if (obj instanceof IRepositoryElement<?>)
			return ((IRepositoryElement<?>) obj).getLocation();
		return null;
	}

}
