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
package org.eclipse.equinox.internal.provisional.p2.ui;

import java.lang.ref.SoftReference;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;

public class IUPropertyUtils {

	// TODO: these constants should come from API, eg. IInstallableUnit or ???
	static final Locale DEFAULT_LOCALE = new Locale("df", "LT"); //$NON-NLS-1$//$NON-NLS-2$
	static final String NAMESPACE_IU_LOCALIZATION = "org.eclipse.equinox.p2.localization"; //$NON-NLS-1$

	// Cache the IU fragments that provide localizations for a given locale.
	//    map: locale => soft reference to a collector
	private static Map LocaleCollectorCache = new HashMap(2);

	// Get the license in the default locale.
	public static ILicense getLicense(IInstallableUnit iu) {
		return getLicense(iu, getCurrentLocale());
	}

	// Get the copyright in the default locale.
	public static ICopyright getCopyright(IInstallableUnit iu) {
		return getCopyright(iu, getCurrentLocale());
	}

	// Get a property in the default locale
	public static String getIUProperty(IInstallableUnit iu, String propertyKey) {
		return getIUProperty(iu, propertyKey, getCurrentLocale());
	}

	public static ILicense getLicense(IInstallableUnit iu, Locale locale) {
		ILicense license = iu.getLicense();
		String body = (license != null ? license.getBody() : null);
		if (body == null || body.length() <= 1 || body.charAt(0) != '%')
			return license;
		final String actualKey = body.substring(1); // Strip off the %
		body = getLocalizedIUProperty(iu, actualKey, locale);
		return MetadataFactory.createLicense(license.getLocation(), body);
	}

	public static ICopyright getCopyright(IInstallableUnit iu, Locale locale) {
		ICopyright copyright = iu.getCopyright();
		String body = (copyright != null ? copyright.getBody() : null);
		if (body == null || body.length() <= 1 || body.charAt(0) != '%')
			return copyright;
		final String actualKey = body.substring(1); // Strip off the %
		body = getLocalizedIUProperty(iu, actualKey, locale);
		return MetadataFactory.createCopyright(copyright.getLocation(), body);
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

		Collector localizationFragments = getLocalizationFragments(locale, locales);

		Collector hostLocalizationCollector = new Collector() {
			public boolean accept(Object object) {
				boolean haveHost = false;
				if (object instanceof IInstallableUnitFragment) {
					IInstallableUnitFragment fragment = (IInstallableUnitFragment) object;
					IRequiredCapability[] hosts = fragment.getHost();
					for (int i = 0; i < hosts.length; i++) {
						IRequiredCapability nextHost = hosts[i];
						if (IInstallableUnit.NAMESPACE_IU_ID.equals(nextHost.getNamespace()) && //
								theUnit.getId().equals(nextHost.getName()) && //
								nextHost.getRange() != null && //
								nextHost.getRange().isIncluded(theUnit.getVersion())) {
							haveHost = true;
							break;
						}
					}
				}
				return (haveHost ? super.accept(object) : true);
			}
		};

		IUPropertyQuery iuQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_FRAGMENT, "true"); //$NON-NLS-1$
		Collector collected = iuQuery.perform(localizationFragments.iterator(), hostLocalizationCollector);

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
	private static String cacheResult(IInstallableUnit iu, String localizedKey, String localizedValue) {
		if (iu instanceof InstallableUnit)
			((InstallableUnit) iu).setLocalizedProperty(localizedKey, localizedValue);
		return localizedValue;
	}

	/**
	 * Collects the installable unit fragments that contain locale data for the given locales.
	 */
	private static synchronized Collector getLocalizationFragments(Locale locale, List localeVariants) {
		SoftReference collectorRef = (SoftReference) LocaleCollectorCache.get(locale);
		if (collectorRef != null) {
			Collector cached = (Collector) collectorRef.get();
			if (cached != null)
				return cached;
		}

		final List locales = localeVariants;

		Collector localeFragmentCollector = new Collector() {
			public boolean accept(Object object) {
				boolean haveLocale = false;
				if (object instanceof IInstallableUnitFragment) {
					IInstallableUnitFragment fragment = (IInstallableUnitFragment) object;
					IProvidedCapability[] provides = fragment.getProvidedCapabilities();
					for (int j = 0; j < provides.length && !haveLocale; j++) {
						IProvidedCapability nextProvide = provides[j];
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
				return (haveLocale ? super.accept(object) : true);
			}
		};

		//Due to performance problems we restrict locale lookup to the current profile (see bug 233958)
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			LogHelper.log(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, "Profile registry unavailable. Default language will be used.", new RuntimeException())); //$NON-NLS-1$
			return new Collector();
		}
		IProfile profile = profileRegistry.getProfile(IProfileRegistry.SELF);
		if (profile == null) {
			LogHelper.log(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, "Profile unavailable. Default language will be used.", new RuntimeException())); //$NON-NLS-1$
			return new Collector();
		}
		IUPropertyQuery iuQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_FRAGMENT, "true"); //$NON-NLS-1$
		Collector collected = profile.query(iuQuery, localeFragmentCollector, null);
		LocaleCollectorCache.put(locale, new SoftReference(collected));
		return collected;
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
