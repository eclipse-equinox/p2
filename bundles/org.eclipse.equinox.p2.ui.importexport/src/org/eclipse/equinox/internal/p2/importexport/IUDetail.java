/*******************************************************************************
 * Copyright (c) 2011, 2017 WindRiver Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
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
