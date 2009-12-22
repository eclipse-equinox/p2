/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.lang.ref.SoftReference;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.query.*;
import org.eclipse.osgi.service.localization.LocaleProvider;

/**
 * TranslationSupport provides string translations for properties of an 
 * IInstallableUnit.  Clients can specify an {@link IQueryable} that should be used
 * to obtain the translation fragment IU's, as well as the locale that
 * should be used for translations.
 * 
 * @since 2.0
 */
public class TranslationSupport {
	// TODO: these constants should come from API, eg. IInstallableUnit or ???
	static final Locale DEFAULT_LOCALE = new Locale("df", "LT"); //$NON-NLS-1$//$NON-NLS-2$
	private static TranslationSupport instance;

	static final String NAMESPACE_IU_LOCALIZATION = "org.eclipse.equinox.p2.localization"; //$NON-NLS-1$
	private IQueryable fragmentSource;

	// Cache the IU fragments that provide localizations for a given locale.
	// Map<String,SoftReference<IQueryResult>>: locale => soft reference to a queryResult
	private Map localeCollectorCache = new HashMap(2);

	private LocaleProvider localeProvider;

	public synchronized static TranslationSupport getInstance() {
		if (instance == null)
			instance = new TranslationSupport();
		return instance;
	}

	/**
	 * Create an instance of TranslationSupport for the current locale.
	 * Unless otherwise specified, the currently running profile will serve
	 * as the source of the translation fragments.
	 * 
	 * @since 2.0
	 */
	private TranslationSupport() {
		super();
		//no instantiate
	}

	/**
	 */
	private List buildLocaleVariants(String locale) {
		ArrayList result = new ArrayList(4);
		int lastSeparator;
		while (true) {
			result.add(locale);
			lastSeparator = locale.lastIndexOf('_');
			if (lastSeparator == -1)
				break;
			locale = locale.substring(0, lastSeparator);
		}
		// Add the default locale (most general)
		result.add(DEFAULT_LOCALE.toString());
		return result;
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
	 * Return the copyright for the specified IInstallableUnit, 
	 * localized for the receiver's locale.
	 * 
	 * @param iu the IInstallableUnit in question
	 * @return the localized copyright defined by the IInstallableUnit
	 */
	public ICopyright getCopyright(IInstallableUnit iu, String locale) {
		if (locale == null)
			locale = getCurrentLocale();
		ICopyright copyright = iu.getCopyright();
		String body = (copyright != null ? copyright.getBody() : null);
		if (body == null || body.length() <= 1 || body.charAt(0) != '%')
			return copyright;
		final String actualKey = body.substring(1); // Strip off the %
		body = getLocalizedIUProperty(iu, actualKey, locale);
		return MetadataFactory.createCopyright(copyright.getLocation(), body);
	}

	private String getCurrentLocale() {
		if (localeProvider != null)
			return localeProvider.getLocale().toString();
		return Locale.getDefault().toString();
	}

	/**
	 * Return the localized value for the specified IInstallableUnit
	 * property.
	 * 
	 * @param iu the IInstallableUnit in question
	 * @param propertyKey the name of the property to be retrieved
	 * @param locale The locale to return the property for
	 * @return the localized property value, or <code>null</code> if no
	 * such property is defined.
	 */
	public String getIUProperty(IInstallableUnit iu, String propertyKey, String locale) {
		if (locale == null)
			locale = getCurrentLocale();
		String value = iu.getProperty(propertyKey);
		if (value == null || value.length() <= 1 || value.charAt(0) != '%')
			return value;
		// else have a localizable property
		final String actualKey = value.substring(1); // Strip off the %
		return getLocalizedIUProperty(iu, actualKey, locale);
	}

	private ILicense getLicense(IInstallableUnit iu, ILicense license, String locale) {
		String body = (license != null ? license.getBody() : null);
		if (body == null || body.length() <= 1 || body.charAt(0) != '%')
			return license;
		final String actualKey = body.substring(1); // Strip off the %
		body = getLocalizedIUProperty(iu, actualKey, locale);
		return MetadataFactory.createLicense(license.getLocation(), body);
	}

	/**
	 * Return an array of licenses for the specified IInstallableUnit, 
	 * localized for the receiver's locale.
	 * 
	 * @param iu the IInstallableUnit in question
	 * @return the localized licenses defined by the IInstallableUnit
	 */
	public ILicense[] getLicenses(IInstallableUnit iu, String locale) {
		if (locale == null)
			locale = getCurrentLocale();
		ILicense[] licenses = iu.getLicenses();
		ILicense[] translatedLicenses = new ILicense[licenses.length];
		for (int i = 0; i < licenses.length; i++) {
			translatedLicenses[i] = getLicense(iu, licenses[i], locale);
		}
		return translatedLicenses;
	}

	/**
	 * Collects the installable unit fragments that contain locale data for the given locales.
	 */
	private synchronized IQueryResult getLocalizationFragments(List localeVariants, String locale) {
		if (fragmentSource == null) {
			LogHelper.log(new Status(IStatus.ERROR, MetadataActivator.PI_METADATA, "Profile registry unavailable. Default language will be used.", new RuntimeException())); //$NON-NLS-1$
			return Collector.EMPTY_COLLECTOR;
		}

		SoftReference queryResultReference = (SoftReference) localeCollectorCache.get(locale);
		if (queryResultReference != null) {
			Collector cached = (Collector) queryResultReference.get();
			if (cached != null)
				return cached;
		}

		final List locales = localeVariants;

		IQuery[] localeQuery = new IQuery[locales.size()];
		for (int j = 0; j < locales.size(); j++) {
			localeQuery[j] = new RequiredCapability(NAMESPACE_IU_LOCALIZATION, (String) locales.get(j), VersionRange.emptyRange, null, false, false);
		}

		IQuery iuQuery = new PipedQuery(new IQuery[] {new FragmentQuery(), CompoundQuery.createCompoundQuery(localeQuery, false)});
		IQueryResult collected = fragmentSource.query(iuQuery, null);
		localeCollectorCache.put(locale, new SoftReference(collected));
		return collected;
	}

	private String getLocalizedIUProperty(IInstallableUnit iu, String actualKey, String locale) {
		String localizedKey = makeLocalizedKey(actualKey, locale);
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

		IQueryResult localizationFragments = getLocalizationFragments(locales, locale);

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

		IQuery iuQuery = new PipedQuery(new IQuery[] {new FragmentQuery(), hostLocalizationQuery});
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

	private String makeLocalizedKey(String actualKey, String localeImage) {
		return localeImage + '.' + actualKey;
	}

	/**
	 * Set the locale that should be used when obtaining translations.
	 * @param provider the locale for which translations should be retrieved.
	 */
	public void setLocaleProvider(LocaleProvider provider) {
		this.localeProvider = provider;
	}

	/**
	 * Set the {@link IQueryable} that should be used to obtain translation fragment
	 * IUs. Returns the previous translation source.
	 * 
	 * @param queryable an {@link IQueryable} that can supply the appropriate NLS
	 * translation fragments
	 */
	public IQueryable setTranslationSource(IQueryable queryable) {
		IQueryable previous = fragmentSource;
		this.fragmentSource = queryable;
		return previous;
	}
}
