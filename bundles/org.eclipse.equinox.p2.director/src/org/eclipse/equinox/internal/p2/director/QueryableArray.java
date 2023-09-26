/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc. - query indexes
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.internal.p2.metadata.index.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.KeyWithLocale;
import org.eclipse.equinox.p2.metadata.index.IIndex;

public class QueryableArray extends IndexProvider<IInstallableUnit> {
	private final List<IInstallableUnit> dataSet;
	private IIndex<IInstallableUnit> capabilityIndex;
	private IIndex<IInstallableUnit> idIndex;
	private TranslationSupport translationSupport;

	public QueryableArray(IInstallableUnit[] ius) {
		dataSet = CollectionUtils.unmodifiableList(ius);
	}

	public QueryableArray(Collection<IInstallableUnit> ius) {
		dataSet = List.copyOf(ius);
	}

	@Override
	public Iterator<IInstallableUnit> everything() {
		return dataSet.iterator();
	}

	@Override
	public boolean contains(IInstallableUnit element) {
		return dataSet.contains(element);
	}

	@Override
	public synchronized IIndex<IInstallableUnit> getIndex(String memberName) {
		if (InstallableUnit.MEMBER_PROVIDED_CAPABILITIES.equals(memberName)) {
			if (capabilityIndex == null)
				capabilityIndex = new CapabilityIndex(dataSet.iterator());
			return capabilityIndex;
		}
		if (InstallableUnit.MEMBER_ID.equals(memberName)) {
			if (idIndex == null)
				idIndex = new IdIndex(dataSet.iterator());
			return idIndex;
		}
		return null;
	}

	@Override
	public synchronized Object getManagedProperty(Object client, String memberName, Object key) {
		if (!(client instanceof IInstallableUnit))
			return null;
		IInstallableUnit iu = (IInstallableUnit) client;
		if (InstallableUnit.MEMBER_TRANSLATED_PROPERTIES.equals(memberName)) {
			if (translationSupport == null)
				translationSupport = new TranslationSupport(this);
			return key instanceof KeyWithLocale ? translationSupport.getIUProperty(iu, (KeyWithLocale) key) : translationSupport.getIUProperty(iu, key.toString());
		}
		return null;
	}
}
