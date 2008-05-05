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
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.generator.features.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
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
	private static final String DOT_XML = ".xml"; //$NON-NLS-1$
	private static final String SITE = "site"; //$NON-NLS-1$
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
		String path = root.getPath();
		if (path.endsWith(fileName))
			return root;

		if (constainsUpdateSiteFileName(path))
			return new URL(root, fileName);

		if (path.endsWith(DIR_SEPARATOR))
			return new URL(root.toExternalForm() + fileName);
		return new URL(root.toExternalForm() + DIR_SEPARATOR + fileName);
	}

	/*
	 * Return a URL based on the given URL, which points to a site.xml file.
	 */
	private static URL getSiteURL(URL url) throws MalformedURLException {
		String path = url.getPath();
		if (constainsUpdateSiteFileName(path))
			return url;

		if (path.endsWith(DIR_SEPARATOR))
			return new URL(url.toExternalForm() + SITE_FILE);
		return new URL(url.toExternalForm() + DIR_SEPARATOR + SITE_FILE);
	}

	private static boolean constainsUpdateSiteFileName(String path) {
		if (path.endsWith(DOT_XML)) {
			int lastSlash = path.lastIndexOf('/');
			String lastSegment = lastSlash == -1 ? path : path.substring(lastSlash + 1);
			if (lastSegment.indexOf(SITE) != -1)
				return true;
		}
		return false;
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
		InputStream input = null;
		File siteFile = loadSiteFile(location, monitor);
		try {
			DefaultSiteParser siteParser = new DefaultSiteParser();
			Checksum checksum = new CRC32();
			input = new CheckedInputStream(new BufferedInputStream(new FileInputStream(siteFile)), checksum);
			SiteModel siteModel = siteParser.parse(input);
			String checksumString = Long.toString(checksum.getValue());
			result = new UpdateSite(siteModel, getSiteURL(location), checksumString);
			siteCache.put(location.toExternalForm(), result);
			return result;
		} catch (SAXException e) {
			String msg = NLS.bind(Messages.ErrorReadingSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.ErrorReadingSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} finally {
			siteFile.delete();
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/**
	 * Returns a local file containing the contents of the update site at the given location.
	 */
	private static File loadSiteFile(URL location, IProgressMonitor monitor) throws ProvisionException {
		Throwable failure;
		try {
			File siteFile = File.createTempFile("site", ".xml"); //$NON-NLS-1$//$NON-NLS-2$
			OutputStream destination = new BufferedOutputStream(new FileOutputStream(siteFile));
			IStatus transferResult = getTransport().download(getSiteURL(location).toExternalForm(), destination, monitor);
			if (transferResult.isOK())
				return siteFile;
			failure = transferResult.getException();
		} catch (IOException e) {
			failure = e;
		}
		int code = (failure instanceof FileNotFoundException) ? ProvisionException.REPOSITORY_NOT_FOUND : ProvisionException.REPOSITORY_FAILED_READ;
		String msg = NLS.bind(Messages.ErrorReadingSite, location);
		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, code, msg, failure));
	}

	/*
	 * Parse the feature.xml specified by the given input stream and return the feature object.
	 * In case of failure, the failure is logged and null is returned
	 */
	private static Feature parseFeature(FeatureParser featureParser, URL featureURL) {
		File featureFile = null;
		try {
			featureFile = File.createTempFile(FEATURE_TEMP_FILE, JAR_EXTENSION);
			OutputStream destination = new BufferedOutputStream(new FileOutputStream(featureFile));
			IStatus transferResult = getTransport().download(featureURL.toExternalForm(), destination, null);
			if (!transferResult.isOK()) {
				//try the download again in case of transient network problems
				destination = new BufferedOutputStream(new FileOutputStream(featureFile));
				transferResult = getTransport().download(featureURL.toExternalForm(), destination, null);
				if (!transferResult.isOK()) {
					LogHelper.log(new ProvisionException(transferResult));
					return null;
				}
			}
			return featureParser.parse(featureFile);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, featureURL), e));
		} finally {
			if (featureFile != null)
				featureFile.delete();
		}
		return null;
	}

	/*
	 * Throw an exception if the site pointed to by the given URL is not valid.
	 */
	public static void validate(URL url, IProgressMonitor monitor) throws ProvisionException {
		try {
			URL siteURL = getSiteURL(url);
			long lastModified = getTransport().getLastModified(siteURL);
			if (lastModified == 0) {
				String msg = NLS.bind(Messages.ErrorReadingSite, url);
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, null));
			}
		} catch (MalformedURLException e) {
			String msg = NLS.bind(Messages.ErrorReadingSite, url);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
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
		URL base = getBaseURL();

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

			String featureURLString = siteFeature.getURLString();
			url = internalGetURL(base, featureURLString);
			if (url != null)
				return url;
		}

		URL url = getArchiveURL(base, FEATURE_DIR + id + VERSION_SEPARATOR + version + JAR_EXTENSION);
		if (url != null)
			return url;

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
		URL base = getBaseURL();
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

	private URL getBaseURL() {
		URL base = null;
		String siteURLString = site.getLocationURLString();
		if (siteURLString != null) {
			if (!siteURLString.endsWith("/")) //$NON-NLS-1$
				siteURLString += "/"; //$NON-NLS-1$
			base = internalGetURL(location, siteURLString);
		}
		if (base == null)
			base = location;
		return base;
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
	private Feature[] loadFeaturesFromDigest() {
		File digestFile = null;
		if (!featureCache.isEmpty())
			return (Feature[]) featureCache.values().toArray(new Feature[featureCache.size()]);
		try {
			URL digestURL = getDigestURL();
			digestFile = File.createTempFile("digest", ".zip"); //$NON-NLS-1$ //$NON-NLS-2$
			BufferedOutputStream destination = new BufferedOutputStream(new FileOutputStream(digestFile));
			IStatus result = getTransport().download(digestURL.toExternalForm(), destination, null);
			if (!result.isOK())
				return null;
			Feature[] features = new DigestParser().parse(digestFile);
			if (features == null)
				return null;
			for (int i = 0; i < features.length; i++) {
				String key = features[i].getId() + VERSION_SEPARATOR + features[i].getVersion();
				featureCache.put(key, features[i]);
			}
			return features;
		} catch (FileNotFoundException fnfe) {
			// we do not track FNF exceptions as we will fall back to the 
			// standard feature parsing from the site itself, see bug 225587.
		} catch (MalformedURLException e) {
			String msg = NLS.bind(Messages.InvalidRepositoryLocation, location);
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, msg, e));
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingDigest, location), e));
		} finally {
			if (digestFile != null)
				digestFile.delete();
		}
		return null;
	}

	private URL getDigestURL() throws MalformedURLException {
		URL digestBase = location;
		String digestURLString = site.getDigestURLString();
		if (digestURLString != null) {
			if (!digestURLString.endsWith("/")) //$NON-NLS-1$
				digestURLString += "/"; //$NON-NLS-1$
			digestBase = internalGetURL(location, digestURLString);
		}

		return getFileURL(digestBase, "digest.zip"); //$NON-NLS-1$
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
			String key = null;
			if (siteFeature.getFeatureIdentifier() != null && siteFeature.getFeatureVersion() != null) {
				key = siteFeature.getFeatureIdentifier() + VERSION_SEPARATOR + siteFeature.getFeatureVersion();
				if (featureCache.containsKey(key))
					continue;
			}
			URL featureURL = getFeatureURL(siteFeature, siteFeature.getFeatureIdentifier(), siteFeature.getFeatureVersion());
			Feature feature = parseFeature(featureParser, featureURL);
			if (feature == null) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, featureURL)));
			} else {
				if (key == null) {
					siteFeature.setFeatureIdentifier(feature.getId());
					siteFeature.setFeatureVersion(feature.getVersion());
					key = siteFeature.getFeatureIdentifier() + VERSION_SEPARATOR + siteFeature.getFeatureVersion();
				}
				featureCache.put(key, feature);
				loadIncludedFeatures(feature, featureParser);
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

			URL includedFeatureURL = getFeatureURL(null, entry.getId(), entry.getVersion());
			Feature includedFeature = parseFeature(featureParser, includedFeatureURL);
			if (includedFeature == null) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, includedFeatureURL)));
			} else {
				featureCache.put(key, includedFeature);
				loadIncludedFeatures(includedFeature, featureParser);
			}
		}
	}

	private static ECFTransport getTransport() {
		return ECFTransport.getInstance();
	}
}
