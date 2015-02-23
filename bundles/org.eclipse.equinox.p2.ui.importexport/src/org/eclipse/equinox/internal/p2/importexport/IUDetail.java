/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport;

import java.net.URI;
import java.util.List;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class IUDetail implements IAdaptable {

	private final IInstallableUnit iu;
	private final List<URI> referredRepo;

	public IUDetail(IInstallableUnit iu, List<URI> uris) {
		this.iu = iu;
		referredRepo = uris;
	}

	public IInstallableUnit getIU() {
		return iu;
	}

	public List<URI> getReferencedRepositories() {
		return referredRepo;
	}

	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (IInstallableUnit.class.equals(adapter))
			return (T) iu;
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof IUDetail) {
			if (iu.equals(((IUDetail) obj).getIU()))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return iu.hashCode();
	}
}
