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
package org.eclipse.equinox.internal.p2.publisher.features;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.SAXException;

/**
 * @since 1.0
 */
public class UpdateSite {

	private static final String VERSION_SEPARATOR = "_"; //$NON-NLS-1$
	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String FEATURE_DIR = "features/"; //$NON-NLS-1$
	private static final String PLUGIN_DIR = "plugins/"; //$NON-NLS-1$
	private static final String FEATURE_TEMP_FILE = "feature"; //$NON-NLS-1$
	private static final String SITE_FILE = "site.xml"; //$NON-NLS-1$
	private static final String DIR_SEPARATOR = "/"; //$NON-NLS-1$
	private String checksum;
	private URL location;
	private SiteModel site;

	/*
	 * Some variables for caching.
	 */
	// map of String (URL.toExternalForm()) to UpdateSite
	private static Map siteCache = new HashMap();
	// map of String (featureID_featureVersion) to Feature
	private Map featureCache = new HashMap();

	/*
	 * Return a new URL for the given file which is based from the specified root.
	 */
	public static URL getFileURL(URL root, String fileName) throws MalformedURLException {
		if (root.getPath().endsWith(fileName))
			return root;
		if (root.getPath().endsWith(SITE_FILE))
			return new URL(root, fileName);
		if (root.getPath().endsWith(DIR_SEPARATOR))
			return new URL(root.toExternalForm() + fileName);
		return new URL(root.toExternalForm() + DIR_SEPARATOR + fileName);
	}

	/*
	 * Open and return the input stream for the given URL.
	 */
	private static InputStream getSiteInputStream(URL url) throws ProvisionException {
		try {
			return getSiteURL(url).openStream();
		} catch (MalformedURLException e) {
			String msg = NLS.bind(Messages.InvalidRepositoryLocation, url);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, msg, e));
		} catch (IllegalArgumentException e) {
			//see bug 221600 - URL.openStream can throw IllegalArgumentException
			String msg = NLS.bind(Messages.InvalidRepositoryLocation, url);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.ErrorReadingSite, url);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, e));
		}
	}

	/*
	 * Return a URL based on the given URL, which points to a site.xml file.
	 */
	private static URL getSiteURL(URL url) throws MalformedURLException {
		if (url.getPath().endsWith(SITE_FILE))
			return url;
		if (url.getPath().endsWith(DIR_SEPARATOR))
			return new URL(url.toExternalForm() + SITE_FILE);
		return new URL(url.toExternalForm() + DIR_SEPARATOR + SITE_FILE);
	}

	/*
	 * Load and return an update site object from the given location.
	 */
	public static UpdateSite load(URL location, IProgressMonitor monitor) throws ProvisionException {
		if (location == null)
			return null;
		UpdateSite result = (UpdateSite) siteCache.get(location.toExternalForm());
		if (result != null)
			return result;
		InputStream input = getSiteInputStream(location);
		try {
			DefaultSiteParser siteParser = new DefaultSiteParser();
			Checksum checksum = new CRC32();
			input = new CheckedInputStream(new BufferedInputStream(input), checksum);
			SiteModel siteModel = siteParser.parse(input);
			String checksumString = Long.toString(checksum.getValue());
			result = new UpdateSite(siteModel, location, checksumString);
			siteCache.put(location.toExternalForm(), result);
			return result;
		} catch (SAXException e) {
			String msg = NLS.bind(Messages.ErrorReadingSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.ErrorReadingSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/*
	 * Parse the feature.xml specified by the given input stream and return the feature object.
	 */
	private static Feature parseFeature(FeatureParser featureParser, URL featureURL) throws IOException, FileNotFoundException, ProvisionException {
		File featureFile = File.createTempFile(FEATURE_TEMP_FILE, JAR_EXTENSION);
		try {
			FileUtils.copyStream(featureURL.openStream(), true, new BufferedOutputStream(new FileOutputStream(featureFile)), true);
			return featureParser.parse(featureFile);
		} catch (IllegalArgumentException e) {
			//see bug 221600 - URL.openStream can throw IllegalArgumentException
			String msg = NLS.bind(Messages.InvalidRepositoryLocation, featureURL.toExternalForm());
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, msg, e));
		} finally {
			featureFile.delete();
		}
	}

	/*
	 * Throw an exception if the site pointed to by the given URL is not valid.
	 */
	public static void validate(URL url, IProgressMonitor monitor) throws ProvisionException {
		InputStream input = getSiteInputStream(url);
		try {
			input.close();
		} catch (IOException e) {
			// ignore
		}
	}

	/*
	 * Constructor for the class.
	 */
	private UpdateSite(SiteModel site, URL location, String checksum) {
		super();
		this.site = site;
		this.location = location;
		this.checksum = checksum;
	}

	/*
	 * Iterate over the archive entries in this site and return the matching URL string for
	 * the given identifier, if there is one.
	 */
	private URL getArchiveURL(URL base, String identifier) {
		URLEntry[] archives = site.getArchives();
		for (int i = 0; archives != null && i < archives.length; i++) {
			URLEntry entry = archives[i];
			if (identifier.equals(entry.getAnnotation()))
				return internalGetURL(base, entry.getURL());
		}
		return null;
	}

	/*
	 * Return the checksum for this site.
	 */
	public String getChecksum() {
		return checksum;
	}

	/*
	 * Return a URL which represents the location of the given feature.
	 */
	public URL getFeatureURL(SiteFeature siteFeature, String id, String version) {
		URL base = site.getLocationURL();
		if (base == null)
			base = location;
		if (siteFeature == null) {
			SiteFeature[] entries = site.getFeatures();
			for (int i = 0; i < entries.length; i++) {
				if (id.equals(entries[i].getFeatureIdentifier()) && version.equals(entries[i].getFeatureVersion())) {
					siteFeature = entries[i];
					break;
				}
			}
		}
		if (siteFeature != null) {
			URL url = siteFeature.getURL();
			if (url != null)
				return url;
			url = getArchiveURL(base, id);
			if (url != null)
				return url;
		}
		// fall through to default URL
		try {
			return getFileURL(base, FEATURE_DIR + id + VERSION_SEPARATOR + version + JAR_EXTENSION);
		} catch (MalformedURLException e) {
			// shouldn't happen
		}
		return null;
	}

	/*
	 * Return the location of this site.
	 */
	public URL getLocation() {
		return location;
	}

	/*
	 * Return a URL which represents the location of the given plug-in.
	 */
	public URL getPluginURL(FeatureEntry plugin) {
		URL base = site.getLocationURL();
		if (base == null)
			base = location;
		String path = PLUGIN_DIR + plugin.getId() + VERSION_SEPARATOR + plugin.getVersion() + JAR_EXTENSION;
		URL url = getArchiveURL(base, path);
		if (url != null)
			return url;
		try {
			return getFileURL(base, path);
		} catch (MalformedURLException e) {
			// shouldn't happen
		}
		return null;
	}

	/*
	 * Return the site model.
	 */
	public SiteModel getSite() {
		return site;
	}

	/*
	 * The trailing parameter can be either null, relative or absolute. If it is null,
	 * then return null. If it is absolute, then create a new url and return it. If it is
	 * relative, then make it relative to the given base url.
	 */
	private URL internalGetURL(URL base, String trailing) {
		if (trailing == null)
			return null;
		try {
			return new URL(trailing);
		} catch (MalformedURLException e) {
			try {
				return new URL(base, trailing);
			} catch (MalformedURLException e2) {
				// shouldn't happen
			}
		}
		return null;
	}

	/*
	 * Load and return the features references in this update site.
	 */
	public Feature[] loadFeatures() throws ProvisionException {
		Feature[] result = loadFeaturesFromDigest();
		return result == null ? loadFeaturesFromSite() : result;
	}

	/*
	 * Try and load the feature information from the update site's
	 * digest file, if it exists.
	 */
	private Feature[] loadFeaturesFromDigest() throws ProvisionException {
		if (!featureCache.isEmpty())
			return (Feature[]) featureCache.values().toArray(new Feature[featureCache.size()]);
		try {
			URL digestURL = getFileURL(location, "digest.zip"); //$NON-NLS-1$
			File digestFile = File.createTempFile("digest", ".zip"); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				FileUtils.copyStream(digestURL.openStream(), true, new BufferedOutputStream(new FileOutputStream(digestFile)), true);
				Feature[] result = new DigestParser().parse(digestFile);
				if (result == null)
					return null;
				for (int i = 0; i < result.length; i++) {
					String key = result[i].getId() + VERSION_SEPARATOR + result[i].getVersion();
					featureCache.put(key, result[i]);
				}
				return result;
			} catch (IllegalArgumentException e) {
				//see bug 221600 - URL.openStream can throw IllegalArgumentException
				String msg = NLS.bind(Messages.InvalidRepositoryLocation, digestURL.toExternalForm());
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, msg, e));
			} finally {
				digestFile.delete();
			}
		} catch (MalformedURLException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingDigest, location), e));
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingDigest, location), e));
		}
		return null;
	}

	/*
	 * Load and return the features that are referenced by this update site. Note this
	 * requires downloading and parsing the feature manifest locally.
	 */
	private Feature[] loadFeaturesFromSite() throws ProvisionException {
		SiteFeature[] siteFeatures = site.getFeatures();
		FeatureParser featureParser = new FeatureParser();
		for (int i = 0; i < siteFeatures.length; i++) {
			SiteFeature siteFeature = siteFeatures[i];
			String key = siteFeature.getFeatureIdentifier() + VERSION_SEPARATOR + siteFeature.getFeatureVersion();
			if (featureCache.containsKey(key))
				continue;
			URL featureURL = getFeatureURL(siteFeature, siteFeature.getFeatureIdentifier(), siteFeature.getFeatureVersion());
			try {
				Feature feature = parseFeature(featureParser, featureURL);
				if (feature == null) {
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, featureURL)));
				} else {
					featureCache.put(key, feature);
					loadIncludedFeatures(feature, featureParser);
				}
			} catch (IOException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, featureURL), e));
			}
		}
		return (Feature[]) featureCache.values().toArray(new Feature[featureCache.size()]);
	}

	/*
	 * Load the features that are included by the given feature.
	 */
	private void loadIncludedFeatures(Feature feature, FeatureParser featureParser) throws ProvisionException {
		FeatureEntry[] featureEntries = feature.getEntries();
		for (int i = 0; i < featureEntries.length; i++) {
			FeatureEntry entry = featureEntries[i];
			if (entry.isRequires() || entry.isPlugin())
				continue;
			String key = entry.getId() + VERSION_SEPARATOR + entry.getVersion();
			if (featureCache.containsKey(key))
				continue;
			URL featureURL = null;
			try {
				featureURL = getFileURL(location, FEATURE_DIR + entry.getId() + VERSION_SEPARATOR + entry.getVersion() + JAR_EXTENSION);
				Feature includedFeature = parseFeature(featureParser, featureURL);
				if (feature == null) {
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, featureURL)));
				} else {
					featureCache.put(key, includedFeature);
					loadIncludedFeatures(includedFeature, featureParser);
				}
			} catch (MalformedURLException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, entry.getId()), e));
			} catch (IOException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, featureURL), e));
			}
		}
	}
}
