/*******************************************************************************
 * Copyright (c) 2008-2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.lang.ref.SoftReference;
import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.query.*;

public class IUPropertyUtils {

	IQueryable queryable;

	/**
	 * 
	 */
	public IUPropertyUtils(IQueryable queryable) {
		this.queryable = queryable;
	}

	// TODO: these constants should come from API, eg. IInstallableUnit or ???
	final Locale DEFAULT_LOCALE = new Locale("df", "LT"); //$NON-NLS-1$//$NON-NLS-2$
	final String NAMESPACE_IU_LOCALIZATION = "org.eclipse.equinox.p2.localization"; //$NON-NLS-1$

	// Cache the IU fragments that provide localizations for a given locale.
	//    map: locale => soft reference to a QueryResult
	private Map LocaleCollectorCache = new HashMap(2);

	// Get the license in the default locale.
	public ILicense[] getLicense(IInstallableUnit iu) {
		return getLicenses(iu, getCurrentLocale());
	}

	// Get the copyright in the default locale.
	public ICopyright getCopyright(IInstallableUnit iu) {
		return getCopyright(iu, getCurrentLocale());
	}

	// Get a property in the default locale
	public String getIUProperty(IInstallableUnit iu, String propertyKey) {
		return getIUProperty(iu, propertyKey, getCurrentLocale());
	}

	public ILicense getLicense(IInstallableUnit iu, ILicense license, Locale locale) {
		String body = (license != null ? license.getBody() : null);
		if (body == null || body.length() <= 1 || body.charAt(0) != '%')
			return license;
		final String actualKey = body.substring(1); // Strip off the %
		body = getLocalizedIUProperty(iu, actualKey, locale);
		return MetadataFactory.createLicense(license.getLocation(), body);
	}

	public ILicense[] getLicenses(IInstallableUnit iu, Locale locale) {
		List<ILicense> licenses = iu.getLicenses();
		ILicense[] translatedLicenses = new ILicense[licenses.size()];
		for (int i = 0; i < licenses.size(); i++) {
			translatedLicenses[i] = getLicense(iu, licenses.get(i), locale);
		}
		return translatedLicenses;
	}

	public ICopyright getCopyright(IInstallableUnit iu, Locale locale) {
		ICopyright copyright = iu.getCopyright();
		String body = (copyright != null ? copyright.getBody() : null);
		if (body == null || body.length() <= 1 || body.charAt(0) != '%')
			return copyright;
		final String actualKey = body.substring(1); // Strip off the %
		body = getLocalizedIUProperty(iu, actualKey, locale);
		return MetadataFactory.createCopyright(copyright.getLocation(), body);
	}

	public String getIUProperty(IInstallableUnit iu, String propertyKey, Locale locale) {
		String value = iu.getProperty(propertyKey);
		if (value == null || value.length() <= 1 || value.charAt(0) != '%')
			return value;
		// else have a localizable property
		final String actualKey = value.substring(1); // Strip off the %
		return getLocalizedIUProperty(iu, actualKey, locale);
	}

	private String getLocalizedIUProperty(IInstallableUnit iu, String actualKey, Locale locale) {
		String localizedKey = makeLocalizedKey(actualKey, locale.toString());
		String localizedValue = null;

		//first check for a cached localized value
		if (iu instanceof InstallableUnit)
			localizedValue = ((InstallableUnit) iu).getLocalizedProperty(localizedKey);
		//next check if the localized value is stored in the same IU (common case)
		if (localizedValue == null)
			localizedValue = iu.getProperty(localizedKey);
		if (localizedValue != null)
			return localizedValue;

		final List locales = buildLocaleVariants(locale);
		final IInstallableUnit theUnit = iu;

		IQueryResult localizationFragments = getLocalizationFragments(locale, locales);

		MatchQuery hostLocalizationQuery = new MatchQuery() {
			public boolean isMatch(Object object) {
				boolean haveHost = false;
				if (object instanceof IInstallableUnitFragment) {
					IInstallableUnitFragment fragment = (IInstallableUnitFragment) object;
					IRequirement[] hosts = fragment.getHost();
					for (int i = 0; i < hosts.length; i++) {
						if (theUnit.satisfies(hosts[i])) {
							haveHost = true;
							break;
						}
					}
				}
				return haveHost;
			}
		};

		IQuery iuQuery = new PipedQuery(new FragmentQuery(), hostLocalizationQuery);
		IQueryResult collected = iuQuery.perform(localizationFragments.iterator());

		if (!collected.isEmpty()) {
			String translation = null;
			for (Iterator iter = collected.iterator(); iter.hasNext() && translation == null;) {
				IInstallableUnit localizationIU = (IInstallableUnit) iter.next();
				for (Iterator jter = locales.iterator(); jter.hasNext();) {
					String localeKey = makeLocalizedKey(actualKey, (String) jter.next());
					translation = localizationIU.getProperty(localeKey);
					if (translation != null)
						return cacheResult(iu, localizedKey, translation);
				}
			}
		}

		for (Iterator iter = locales.iterator(); iter.hasNext();) {
			String nextLocale = (String) iter.next();
			String localeKey = makeLocalizedKey(actualKey, nextLocale);
			String nextValue = iu.getProperty(localeKey);
			if (nextValue != null)
				return cacheResult(iu, localizedKey, nextValue);
		}

		return cacheResult(iu, localizedKey, actualKey);
	}

	/**
	 * Cache the translated property value to optimize future retrieval of the same value.
	 * Currently we just cache on the installable unit object in memory. In future
	 * we should push support for localized property retrieval into IInstallableUnit
	 * so we aren't required to reach around the API here.
	 */
	private String cacheResult(IInstallableUnit iu, String localizedKey, String localizedValue) {
		if (iu instanceof InstallableUnit)
			((InstallableUnit) iu).setLocalizedProperty(localizedKey, localizedValue);
		return localizedValue;
	}

	/**
	 * Collects the installable unit fragments that contain locale data for the given locales.
	 */
	private synchronized IQueryResult getLocalizationFragments(Locale locale, List localeVariants) {
		SoftReference queryResultRef = (SoftReference) LocaleCollectorCache.get(locale);
		if (queryResultRef != null) {
			Collector cached = (Collector) queryResultRef.get();
			if (cached != null)
				return cached;
		}

		final List locales = localeVariants;

		MatchQuery localeFragmentQuery = new MatchQuery() {
			public boolean isMatch(Object object) {
				boolean haveLocale = false;
				if (object instanceof IInstallableUnitFragment) {
					IInstallableUnitFragment fragment = (IInstallableUnitFragment) object;
					Collection<IProvidedCapability> provides = fragment.getProvidedCapabilities();
					Iterator<IProvidedCapability> it = provides.iterator();
					while (it.hasNext() && !haveLocale) {
						IProvidedCapability nextProvide = it.next();
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
				return haveLocale;
			}
		};

		IQuery iuQuery = new PipedQuery(new FragmentQuery(), localeFragmentQuery);
		IQueryResult collected = queryable.query(iuQuery, null);
		LocaleCollectorCache.put(locale, new SoftReference(collected));
		return collected;
	}

	/**
	 */
	private List buildLocaleVariants(Locale locale) {
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

	private String makeLocalizedKey(String actualKey, String localeImage) {
		return localeImage + '.' + actualKey;
	}

	private Locale getCurrentLocale() {
		return Locale.getDefault();
	}

}
