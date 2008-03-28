/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.query;

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;

public class IUPropertyUtils {

	static final Locale DEFAULT_LOCALE = new Locale("df", "LT"); //$NON-NLS-1$//$NON-NLS-2$

	public static String getIUProperty(IInstallableUnit iu, String propertyKey, Locale locale) {
		String value = iu.getProperty(propertyKey);
		if (value == null || value.length() <= 1 || value.charAt(0) != '%')
			return value;
		// else have a localizable property
		String actualKey = value.substring(1);
		String localizationBundles[] = buildLocalizationVariants(iu, locale);

		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());

		for (int i = 0; i < localizationBundles.length; i++) {
			InstallableUnitQuery iuQuery = new InstallableUnitQuery(localizationBundles[i]);
			Collector collected = repoMgr.query(iuQuery, new Collector(), null);
			if (!collected.isEmpty()) {
				for (Iterator iter = collected.iterator(); iter.hasNext();) {
					IInstallableUnit localizationIU = (IInstallableUnit) iter.next();
					String translation = localizationIU.getProperty(actualKey);
					if (translation != null)
						return translation;
				}
			}
		}

		String defaultKey = DEFAULT_LOCALE.toString() + '.' + actualKey;
		String defaultValue = iu.getProperty(defaultKey);
		if (defaultValue != null)
			return defaultValue;

		return value;
	}

	private static final String TRANSLATED_PROPERTIES_NAME = "_translated_properties"; //$NON-NLS-1$

	/**
	 */
	private static String[] buildLocalizationVariants(IInstallableUnit iu, Locale locale) {
		String id = iu.getId().toString();
		String nl = locale.toString();
		ArrayList result = new ArrayList(4);
		int lastSeparator;
		while (true) {
			result.add(id + '_' + nl + TRANSLATED_PROPERTIES_NAME);
			lastSeparator = nl.lastIndexOf('_');
			if (lastSeparator == -1)
				break;
			nl = nl.substring(0, lastSeparator);
		}
		// Add the empty suffix last (most general)
		result.add(id + TRANSLATED_PROPERTIES_NAME);
		return (String[]) result.toArray(new String[result.size()]);
	}

}
