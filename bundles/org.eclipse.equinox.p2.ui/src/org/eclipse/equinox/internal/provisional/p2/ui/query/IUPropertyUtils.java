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

import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;

public class IUPropertyUtils {

	// TODO: these constants should come from API, eg. IInstallableUnit or ???
	static final Locale DEFAULT_LOCALE = new Locale("df", "LT"); //$NON-NLS-1$//$NON-NLS-2$
	static final String NAMESPACE_IU_LOCALIZATION = "org.eclipse.equinox.p2.localization"; //$NON-NLS-1$

	// Get the license in the default locale.
	public static License getLicense(IInstallableUnit iu) {
		return getLicense(iu, getCurrentLocale());
	}

	// Get the copyright in the default locale.
	public static Copyright getCopyright(IInstallableUnit iu) {
		return getCopyright(iu, getCurrentLocale());
	}

	// Get a property in the default locale
	public static String getIUProperty(IInstallableUnit iu, String propertyKey) {
		return getIUProperty(iu, propertyKey, getCurrentLocale());
	}

	public static License getLicense(IInstallableUnit iu, Locale locale) {
		License license = iu.getLicense();
		String body = (license != null ? license.getBody() : null);
		if (body == null || body.length() <= 1 || body.charAt(0) != '%')
			return license;
		final String actualKey = body.substring(1); // Strip off the %
		body = getLocalizedIUProperty(iu, actualKey, locale);
		URL url = license.getURL();
		return new License((url != null ? url.toExternalForm() : null), body);
	}

	public static Copyright getCopyright(IInstallableUnit iu, Locale locale) {
		Copyright copyright = iu.getCopyright();
		String body = (copyright != null ? copyright.getBody() : null);
		if (body == null || body.length() <= 1 || body.charAt(0) != '%')
			return copyright;
		final String actualKey = body.substring(1); // Strip off the %
		body = getLocalizedIUProperty(iu, actualKey, locale);
		URL url = copyright.getURL();
		return new Copyright((url != null ? url.toExternalForm() : null), body);
	}

	public static String getIUProperty(IInstallableUnit iu, String propertyKey, Locale locale) {
		String value = iu.getProperty(propertyKey);
		if (value == null || value.length() <= 1 || value.charAt(0) != '%')
			return value;
		// else have a localizable property
		final String actualKey = value.substring(1); // Strip off the %
		return getLocalizedIUProperty(iu, actualKey, locale);
	}

	private static String getLocalizedIUProperty(IInstallableUnit iu, String actualKey, Locale locale) {
		// Short circuit query if iu provides value for matching locale	
		String localizedKey = makeLocalizedKey(actualKey, locale.toString());
		String localizedValue = iu.getProperty(localizedKey);
		if (localizedValue != null)
			return localizedValue;

		final List locales = buildLocaleVariants(locale);
		final IInstallableUnit theUnit = iu;

		Collector hostLocalizationCollector = new Collector() {
			public boolean accept(Object object) {
				boolean haveHost = false;
				boolean haveLocale = false;
				if (object instanceof IInstallableUnitFragment) {
					IInstallableUnitFragment fragment = (IInstallableUnitFragment) object;
					RequiredCapability[] hosts = fragment.getHost();
					for (int i = 0; i < hosts.length; i++) {
						RequiredCapability nextHost = hosts[i];
						if (IInstallableUnit.NAMESPACE_IU_ID.equals(nextHost.getNamespace()) && //
								theUnit.getId().equals(nextHost.getName()) && //
								nextHost.getRange() != null && //
								nextHost.getRange().isIncluded(theUnit.getVersion())) {
							haveHost = true;
							break;
						}
					}

					if (haveHost) {
						ProvidedCapability[] provides = fragment.getProvidedCapabilities();
						for (int j = 0; j < provides.length && !haveLocale; j++) {
							ProvidedCapability nextProvide = provides[j];
							if (NAMESPACE_IU_LOCALIZATION.equals(nextProvide.getNamespace())) {
								String providedLocale = nextProvide.getName();
								if (providedLocale != null) {
									for (Iterator iter = locales.iterator(); iter.hasNext();) {
										if (providedLocale.equals(iter.next())) {
											haveLocale = true;
											break;
										}
									}
								}
							}
						}
					}
				}
				return (haveHost && haveLocale ? super.accept(object) : false);
			}
		};

		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		IUPropertyQuery iuQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_FRAGMENT, "true"); //$NON-NLS-1$
		Collector collected = repoMgr.query(iuQuery, hostLocalizationCollector, null);
		if (!collected.isEmpty()) {
			String translation = null;
			for (Iterator iter = collected.iterator(); iter.hasNext() && translation == null;) {
				IInstallableUnit localizationIU = (IInstallableUnit) iter.next();
				for (Iterator jter = locales.iterator(); jter.hasNext();) {
					String localeKey = makeLocalizedKey(actualKey, (String) jter.next());
					translation = localizationIU.getProperty(localeKey);
					if (translation != null)
						return translation;
				}
			}
		}

		for (Iterator iter = locales.iterator(); iter.hasNext();) {
			String nextLocale = (String) iter.next();
			String localeKey = makeLocalizedKey(actualKey, nextLocale);
			String nextValue = iu.getProperty(localeKey);
			if (nextValue != null)
				return nextValue;
		}

		return actualKey;
	}

	/**
	 */
	private static List buildLocaleVariants(Locale locale) {
		String nl = locale.toString();
		ArrayList result = new ArrayList(4);
		int lastSeparator;
		while (true) {
			result.add(nl);
			lastSeparator = nl.lastIndexOf('_');
			if (lastSeparator == -1)
				break;
			nl = nl.substring(0, lastSeparator);
		}
		// Add the default locale (most general)
		result.add(DEFAULT_LOCALE.toString());
		return result;
	}

	private static String makeLocalizedKey(String actualKey, String localeImage) {
		return localeImage + '.' + actualKey;
	}

	private static Locale getCurrentLocale() {
		return Locale.getDefault();
	}

}
