/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - transport split
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
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
	private static final String PROTOCOL_FILE = "file"; //$NON-NLS-1$
	private static final int RETRY_COUNT = 2;
	private static final String DOT_XML = ".xml"; //$NON-NLS-1$
	private static final String SITE = "site"; //$NON-NLS-1$
	private String checksum;
	private URI location;
	private URI rootLocation;
	private SiteModel site;

	/*
	 * Some variables for caching.
	 */
	// map of String (URI.toString()) to UpdateSite
	private static Map<String, SoftReference<UpdateSite>> siteCache = new HashMap<String, SoftReference<UpdateSite>>();
	// map of String (URI.toString()) to UpdateSite (for category xmls)
	private static Map<String, SoftReference<UpdateSite>> categoryCache = new HashMap<String, SoftReference<UpdateSite>>();
	// map of String (featureID_featureVersion) to Feature
	private Map<String, Feature> featureCache = new HashMap<String, Feature>();
	// map of String (bundleID_featureVersion) to BundleDescriptr
	private Map<String, BundleDescription> bundleCache = new HashMap<String, BundleDescription>();
	private Transport transport;

	/*
	 * Return a URI based on the given URI, which points to a site.xml file.
	 */
	private static URI getSiteURI(URI baseLocation) {
		String segment = URIUtil.lastSegment(baseLocation);
		if (constainsUpdateSiteFileName(segment))
			return baseLocation;
		return URIUtil.append(baseLocation, SITE_FILE);
	}

	/**
	 * Be lenient about accepting any location with *site*.xml at the end.
	 */
	private static boolean constainsUpdateSiteFileName(String segment) {
		return segment != null && segment.endsWith(DOT_XML) && segment.indexOf(SITE) != -1;
	}

	/**
	 * Loads and returns a category file
	 * @param location
	 * @param monitor
	 * @return A CategoryFile
	 * @throws ProvisionException
	 */
	public static synchronized UpdateSite loadCategoryFile(URI location, Transport transport, IProgressMonitor monitor) throws ProvisionException {
		if (location == null)
			return null;
		UpdateSite result = null;
		if (!PROTOCOL_FILE.equals(location.getScheme()) && categoryCache.containsKey(location.toString())) {
			result = categoryCache.get(location.toString()).get();
			if (result != null)
				return result;
			//else soft reference has been cleared, take it out of the cache
			categoryCache.remove(location.toString());
		}

		InputStream input = null;
		File siteFile = loadActualSiteFile(location, location, transport, monitor);
		try {
			CategoryParser siteParser = new CategoryParser(location);
			Checksum checksum = new CRC32();
			input = new CheckedInputStream(new BufferedInputStream(new FileInputStream(siteFile)), checksum);
			SiteModel siteModel = siteParser.parse(input);
			String checksumString = Long.toString(checksum.getValue());
			result = new UpdateSite(siteModel, location, transport, checksumString);
			if (!PROTOCOL_FILE.equals(location.getScheme()))
				categoryCache.put(location.toString(), new SoftReference<UpdateSite>(result));
			return result;
		} catch (SAXException e) {
			String msg = NLS.bind(Messages.ErrorReadingSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.ErrorReadingSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				// ignore
			}
			if (!PROTOCOL_FILE.equals(location.getScheme()))
				siteFile.delete();
		}
	}

	/*
	 * Load and return an update site object from the given location.
	 */
	public static synchronized UpdateSite load(URI location, Transport transport, IProgressMonitor monitor) throws ProvisionException {
		if (location == null)
			return null;

		UpdateSite result = null;
		//only caching remote sites
		if (!PROTOCOL_FILE.equals(location.getScheme()) && siteCache.containsKey(location.toString())) {
			result = siteCache.get(location.toString()).get();
			if (result != null)
				return result;
			//else soft reference has been cleared, take it out of the cache
			siteCache.remove(location.toString());
		}

		InputStream input = null;
		File siteFile = loadActualSiteFile(location, getSiteURI(location), transport, monitor);
		try {
			DefaultSiteParser siteParser = new DefaultSiteParser(location);
			Checksum checksum = new CRC32();
			input = new CheckedInputStream(new BufferedInputStream(new FileInputStream(siteFile)), checksum);
			SiteModel siteModel = siteParser.parse(input);
			String checksumString = Long.toString(checksum.getValue());
			result = new UpdateSite(siteModel, getSiteURI(location), transport, checksumString);
			if (!PROTOCOL_FILE.equals(location.getScheme()))
				siteCache.put(location.toString(), new SoftReference<UpdateSite>(result));
			return result;
		} catch (SAXException e) {
			String msg = NLS.bind(Messages.ErrorReadingSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.ErrorReadingSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				// ignore
			}
			if (!PROTOCOL_FILE.equals(location.getScheme()))
				siteFile.delete();
		}
	}

	/**
	 * Returns a local file containing the contents of the update site at the given location.
	 */
	private static File loadActualSiteFile(URI location, URI actualLocation, Transport transport, IProgressMonitor monitor) throws ProvisionException {
		SubMonitor submonitor = SubMonitor.convert(monitor, 1000);
		try {
			File siteFile = null;
			IStatus transferResult = null;
			boolean deleteSiteFile = false;
			try {
				if (PROTOCOL_FILE.equals(actualLocation.getScheme())) {
					siteFile = URIUtil.toFile(actualLocation);
					if (siteFile.exists())
						transferResult = Status.OK_STATUS;
					else {
						String msg = NLS.bind(Messages.ErrorReadingSite, location);
						transferResult = new Status(IStatus.ERROR, Activator.ID, ProvisionException.ARTIFACT_NOT_FOUND, msg, new FileNotFoundException(siteFile.getAbsolutePath()));
					}
				} else {
					// creating a temp file. In the event of an error we want to delete it.
					deleteSiteFile = true;
					OutputStream destination = null;
					try {
						siteFile = File.createTempFile("site", ".xml"); //$NON-NLS-1$//$NON-NLS-2$
						destination = new BufferedOutputStream(new FileOutputStream(siteFile));
					} catch (IOException e) {
						throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, "Can not create tempfile for site.xml", e)); //$NON-NLS-1$
					}
					try {
						transferResult = transport.download(actualLocation, destination, submonitor.newChild(999));
					} finally {
						try {
							destination.close();
						} catch (IOException e) {
							throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, "Failing to close tempfile for site.xml", e)); //$NON-NLS-1$
						}
					}
				}
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				if (transferResult.isOK()) {
					// successful. If the siteFile is the download of a remote site.xml it will get cleaned up later
					deleteSiteFile = false;
					return siteFile;
				}

				// The transferStatus from download has a well formatted message that should
				// be used as it contains useful feedback to the user.
				// The only thing needed is to translate the error code ARTIFACT_NOT_FOUND to
				// REPOSITORY_NOT_FOUND as the download does not know what the file represents.
				//
				IStatus ms = null;
				if (transferResult.getException() instanceof FileNotFoundException)
					ms = new MultiStatus(Activator.ID, //
							ProvisionException.REPOSITORY_NOT_FOUND,
							// (code == ProvisionException.ARTIFACT_NOT_FOUND || code == ProvisionException.REPOSITORY_NOT_FOUND ? ProvisionException.REPOSITORY_NOT_FOUND : ProvisionException.REPOSITORY_FAILED_READ), //
							new IStatus[] {transferResult}, //
							NLS.bind(Messages.ErrorReadingSite, location), null);
				else
					ms = transferResult;
				throw new ProvisionException(ms);

			} finally {
				if (deleteSiteFile && siteFile != null)
					siteFile.delete();
			}
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	/*
	 * Parse the feature.xml specified by the given input stream and return the feature object.
	 * In case of failure, the failure is logged and null is returned
	 */
	private Feature parseFeature(FeatureParser featureParser, URI featureURI, IProgressMonitor monitor) {
		File featureFile = null;
		if (PROTOCOL_FILE.equals(featureURI.getScheme())) {
			featureFile = URIUtil.toFile(featureURI);
			return featureParser.parse(featureFile);
		}
		try {
			featureFile = File.createTempFile(FEATURE_TEMP_FILE, JAR_EXTENSION);
			IStatus transferResult = null;
			//try the download twice in case of transient network problems
			for (int i = 0; i < RETRY_COUNT; i++) {
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				OutputStream destination = new BufferedOutputStream(new FileOutputStream(featureFile));
				try {
					transferResult = transport.download(featureURI, destination, monitor);
				} finally {
					try {
						destination.close();
					} catch (IOException e) {
						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, featureURI), e));
						return null;
					}
				}
				if (transferResult.isOK())
					break;
			}
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			if (!transferResult.isOK()) {
				LogHelper.log(new ProvisionException(transferResult));
				return null;
			}
			return featureParser.parse(featureFile);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, featureURI), e));
		} finally {
			if (featureFile != null)
				featureFile.delete();
		}
		return null;
	}

	/*
	 * Constructor for the class.
	 */
	private UpdateSite(SiteModel site, URI location, Transport transport, String checksum) {
		super();
		this.site = site;
		this.location = location;
		this.checksum = checksum;
		this.rootLocation = getRootLocation();
		this.transport = transport;
	}

	private URI getRootLocation() {
		String locationString = location.toString();
		int slashIndex = locationString.lastIndexOf('/');
		if (slashIndex == -1 || slashIndex == (locationString.length() - 1))
			return location;

		return URI.create(locationString.substring(0, slashIndex + 1));
	}

	/*
	 * Iterate over the archive entries in this site and return the matching URI string for
	 * the given identifier, if there is one.
	 */
	private URI getArchiveURI(URI base, String identifier) {
		URLEntry[] archives = site.getArchives();
		for (int i = 0; archives != null && i < archives.length; i++) {
			URLEntry entry = archives[i];
			if (identifier.equals(entry.getAnnotation()))
				return internalGetURI(base, entry.getURL());
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
	 * Return a URI which represents the location of the given feature.
	 */
	public URI getSiteFeatureURI(SiteFeature siteFeature) {
		URL url = siteFeature.getURL();
		try {
			if (url != null)
				return URIUtil.toURI(url);
		} catch (URISyntaxException e) {
			//fall through and resolve the URI ourselves
		}
		URI base = getBaseURI();
		String featureURIString = siteFeature.getURLString();
		return internalGetURI(base, featureURIString);
	}

	/*
	 * Return a URI which represents the location of the given bundle.
	 */
	public URI getSiteBundleURI(SiteBundle siteBundle) {
		URL url = siteBundle.getURL();
		try {
			if (url != null)
				return URIUtil.toURI(url);
		} catch (URISyntaxException e) {
			//fall through and resolve the URI ourselves
		}
		URI base = getBaseURI();
		String bundleURIString = siteBundle.getURLString();
		return internalGetURI(base, bundleURIString);
	}

	/*
	 * Return a URI which represents the location of the given feature.
	 */
	public URI getFeatureURI(String id, String version) {
		SiteFeature[] entries = site.getFeatures();
		for (int i = 0; i < entries.length; i++) {
			if (id.equals(entries[i].getFeatureIdentifier()) && version.equals(entries[i].getFeatureVersion())) {
				return getSiteFeatureURI(entries[i]);
			}
		}

		URI base = getBaseURI();
		URI url = getArchiveURI(base, FEATURE_DIR + id + VERSION_SEPARATOR + version + JAR_EXTENSION);
		if (url != null)
			return url;
		return URIUtil.append(base, FEATURE_DIR + id + VERSION_SEPARATOR + version + JAR_EXTENSION);
	}

	/*
	 * Return a URI which represents the location of the given bundle.
	 */
	public URI getBundleURI(String id, String version) {
		SiteBundle[] entries = site.getBundles();
		for (int i = 0; i < entries.length; i++) {
			if (id.equals(entries[i].getBundleIdentifier()) && version.equals(entries[i].getBundleVersion())) {
				return getSiteBundleURI(entries[i]);
			}
		}

		URI base = getBaseURI();
		URI url = getArchiveURI(base, FEATURE_DIR + id + VERSION_SEPARATOR + version + JAR_EXTENSION);
		if (url != null)
			return url;
		return URIUtil.append(base, FEATURE_DIR + id + VERSION_SEPARATOR + version + JAR_EXTENSION);
	}

	/*
	 * Return the location of this site.
	 */
	public URI getLocation() {
		return location;
	}

	public String getMirrorsURI() {
		//copy mirror information from update site to p2 repositories
		String mirrors = site.getMirrorsURI();
		if (mirrors == null)
			return null;
		//remove site.xml file reference
		int index = mirrors.indexOf("site.xml"); //$NON-NLS-1$
		if (index != -1)
			mirrors = mirrors.substring(0, index) + mirrors.substring(index + "site.xml".length()); //$NON-NLS-1$
		return mirrors;
	}

	/*
	 * Return a URI which represents the location of the given plug-in.
	 */
	public URI getPluginURI(FeatureEntry plugin) {
		URI base = getBaseURI();
		String path = PLUGIN_DIR + plugin.getId() + VERSION_SEPARATOR + plugin.getVersion() + JAR_EXTENSION;
		URI url = getArchiveURI(base, path);
		if (url != null)
			return url;
		return URIUtil.append(base, path);
	}

	private URI getBaseURI() {
		URI base = null;
		String siteURIString = site.getLocationURIString();
		if (siteURIString != null) {
			if (!siteURIString.endsWith("/")) //$NON-NLS-1$
				siteURIString += "/"; //$NON-NLS-1$
			base = internalGetURI(rootLocation, siteURIString);
		}
		if (base == null)
			base = rootLocation;
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
	private URI internalGetURI(URI base, String trailing) {
		if (trailing == null)
			return null;
		return URIUtil.makeAbsolute(URI.create(trailing), base);
	}

	/*
	 * Load and return the features references in this update site.
	 */
	public synchronized Feature[] loadFeatures(IProgressMonitor monitor) throws ProvisionException {
		if (!featureCache.isEmpty())
			return featureCache.values().toArray(new Feature[featureCache.size()]);
		Feature[] result = loadFeaturesFromDigest(monitor);
		return result == null ? loadFeaturesFromSite(monitor) : result;
	}

	/*
	 * Load and return the bundle references in this update site.
	 */
	public synchronized BundleDescription[] loadBundles(IProgressMonitor monitor) throws ProvisionException {
		if (!bundleCache.isEmpty())
			return bundleCache.values().toArray(new BundleDescription[bundleCache.size()]);
		BundleDescription[] result = null; // TODO loadBundlesFromDigest(monitor);
		return result == null ? loadBundlesFromSite(monitor) : result;
	}

	/*
	 * Try and load the feature information from the update site's
	 * digest file, if it exists.
	 */
	private Feature[] loadFeaturesFromDigest(IProgressMonitor monitor) {
		File digestFile = null;
		boolean local = false;
		try {
			URI digestURI = getDigestURI();
			if (PROTOCOL_FILE.equals(digestURI.getScheme())) {
				digestFile = URIUtil.toFile(digestURI);
				if (!digestFile.exists())
					return null;
				local = true;
			} else {
				digestFile = File.createTempFile("digest", ".zip"); //$NON-NLS-1$ //$NON-NLS-2$
				BufferedOutputStream destination = new BufferedOutputStream(new FileOutputStream(digestFile));
				IStatus result = null;
				try {
					result = transport.download(digestURI, destination, monitor);
				} finally {
					try {
						destination.close();
					} catch (IOException e) {
						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, location), e));
						return null;
					}
				}
				if (result.getSeverity() == IStatus.CANCEL || monitor.isCanceled())
					throw new OperationCanceledException();
				if (!result.isOK())
					return null;
			}
			Feature[] features = new DigestParser().parse(digestFile, digestURI);
			if (features == null)
				return null;
			Map<String, Feature> tmpFeatureCache = new HashMap<String, Feature>(features.length);
			for (int i = 0; i < features.length; i++) {
				String key = features[i].getId() + VERSION_SEPARATOR + features[i].getVersion();
				tmpFeatureCache.put(key, features[i]);
			}
			featureCache = tmpFeatureCache;
			return features;
		} catch (FileNotFoundException fnfe) {
			// we do not track FNF exceptions as we will fall back to the 
			// standard feature parsing from the site itself, see bug 225587.
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingDigest, location), e));
		} finally {
			if (!local && digestFile != null)
				digestFile.delete();
		}
		return null;
	}

	private URI getDigestURI() {
		URI digestBase = null;
		String digestURIString = site.getDigestURIString();
		if (digestURIString != null) {
			if (!digestURIString.endsWith("/")) //$NON-NLS-1$
				digestURIString += "/"; //$NON-NLS-1$
			digestBase = internalGetURI(rootLocation, digestURIString);
		}

		if (digestBase == null)
			digestBase = rootLocation;

		return URIUtil.append(digestBase, "digest.zip"); //$NON-NLS-1$
	}

	/*
	 * Load and return the features that are referenced by this update site. Note this
	 * requires downloading and parsing the feature manifest locally.
	 */
	private Feature[] loadFeaturesFromSite(IProgressMonitor monitor) throws ProvisionException {
		SiteFeature[] siteFeatures = site.getFeatures();
		FeatureParser featureParser = new FeatureParser();
		Map<String, Feature> tmpFeatureCache = new HashMap<String, Feature>(siteFeatures.length);

		for (int i = 0; i < siteFeatures.length; i++) {
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			SiteFeature siteFeature = siteFeatures[i];
			String key = null;
			if (siteFeature.getFeatureIdentifier() != null && siteFeature.getFeatureVersion() != null) {
				key = siteFeature.getFeatureIdentifier() + VERSION_SEPARATOR + siteFeature.getFeatureVersion();
				if (tmpFeatureCache.containsKey(key))
					continue;
			}
			URI featureURI = getSiteFeatureURI(siteFeature);
			Feature feature = parseFeature(featureParser, featureURI, new NullProgressMonitor());
			if (feature == null) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, featureURI)));
			} else {
				if (key == null) {
					siteFeature.setFeatureIdentifier(feature.getId());
					siteFeature.setFeatureVersion(feature.getVersion());
					key = siteFeature.getFeatureIdentifier() + VERSION_SEPARATOR + siteFeature.getFeatureVersion();
				}
				tmpFeatureCache.put(key, feature);
				loadIncludedFeatures(feature, featureParser, tmpFeatureCache, monitor);
			}
		}
		featureCache = tmpFeatureCache;
		return featureCache.values().toArray(new Feature[featureCache.size()]);
	}

	/*
	 * Load the features that are included by the given feature.
	 */
	private void loadIncludedFeatures(Feature feature, FeatureParser featureParser, Map<String, Feature> features, IProgressMonitor monitor) throws ProvisionException {
		FeatureEntry[] featureEntries = feature.getEntries();
		for (int i = 0; i < featureEntries.length; i++) {
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			FeatureEntry entry = featureEntries[i];
			if (entry.isRequires() || entry.isPlugin())
				continue;
			String key = entry.getId() + VERSION_SEPARATOR + entry.getVersion();
			if (features.containsKey(key))
				continue;

			URI includedFeatureURI = getFeatureURI(entry.getId(), entry.getVersion());
			Feature includedFeature = parseFeature(featureParser, includedFeatureURI, monitor);
			if (includedFeature == null) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, includedFeatureURI)));
			} else {
				features.put(key, includedFeature);
				loadIncludedFeatures(includedFeature, featureParser, features, monitor);
			}
		}
	}

	/*
	 * Load and return the bundles that are referenced by this update site. Note this
	 * requires downloading and parsing the feature manifest locally.
	 */
	private BundleDescription[] loadBundlesFromSite(IProgressMonitor monitor) {
		SiteBundle[] siteBundles = site.getBundles();
		Map<String, BundleDescription> tmpBundleCache = new HashMap<String, BundleDescription>(siteBundles.length);

		for (int i = 0; i < siteBundles.length; i++) {
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			SiteBundle siteBundle = siteBundles[i];
			String key = null;
			if (siteBundle.getBundleIdentifier() != null && siteBundle.getBundleVersion() != null) {
				key = siteBundle.getBundleIdentifier() + VERSION_SEPARATOR + siteBundle.getBundleVersion();
				if (tmpBundleCache.containsKey(key))
					continue;
			}
			URI bundleURI = getSiteBundleURI(siteBundle);
			BundleDescription bundle = parseBundleDescription(bundleURI, monitor);
			if (bundle == null) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingBundle, bundleURI)));
			} else {
				if (key == null) {
					siteBundle.setBundleIdentifier(bundle.getSymbolicName());
					siteBundle.setBundleVersion(bundle.getVersion().toString());
					key = siteBundle.getBundleIdentifier() + VERSION_SEPARATOR + siteBundle.getBundleVersion().toString();
				}
				tmpBundleCache.put(key, bundle);
			}
		}
		bundleCache = tmpBundleCache;
		return bundleCache.values().toArray(new BundleDescription[bundleCache.size()]);
	}

	/*
	 * Reads a bundle and extract its BundleDescription
	 * In case of failure, the failure is logged and null is returned
	 */
	private BundleDescription parseBundleDescription(URI bundleURI, IProgressMonitor monitor) {
		File bundleFile = null;
		if (PROTOCOL_FILE.equals(bundleURI.getScheme())) {
			bundleFile = URIUtil.toFile(bundleURI);
		}
		try {
			bundleFile = File.createTempFile("bundle", JAR_EXTENSION); //$NON-NLS-1$
			IStatus transferResult = null;
			//try the download twice in case of transient network problems
			for (int i = 0; i < RETRY_COUNT; i++) {
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				OutputStream destination = new BufferedOutputStream(new FileOutputStream(bundleFile));
				try {
					transferResult = transport.download(bundleURI, destination, monitor);
				} finally {
					try {
						destination.close();
					} catch (IOException e) {
						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingFeature, bundleURI), e));
						return null;
					}
				}
				if (transferResult.isOK())
					break;
			}
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			if (!transferResult.isOK()) {
				LogHelper.log(new ProvisionException(transferResult));
				return null;
			}
			return BundlesAction.createBundleDescriptionIgnoringExceptions(bundleFile);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingBundle, bundleURI), e));
		} finally {
			if (bundleFile != null)
				bundleFile.delete();
		}
		return null;
	}

}
