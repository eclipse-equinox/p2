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
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.net.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.w3c.dom.*;

/**
 * Mirror support class for repositories. This class implements
 * mirror support equivalent to the mirroring of update manager sites. A repository 
 * optionally provides a mirror URL via the {@link IRepository#PROP_MIRRORS_URL} key. 
 * The contents of the file at this URL is expected to be an XML document 
 * containing a list of <mirror> elements. The mirrors are assumed to be already 
 * sorted geographically with closer mirrors first.
 */
public class MirrorSelector {
	private static final double LOG2 = Math.log(2);

	/**
	 * Encapsulates information about a single mirror
	 */
	static class MirrorInfo implements Comparable {
		long bytesPerSecond;
		int failureCount;
		private final int initialRank;
		String locationString;

		MirrorInfo(String location, int initialRank) {
			this.initialRank = initialRank;
			this.locationString = location;
			if (!locationString.endsWith("/")) //$NON-NLS-1$
				locationString = locationString + "/"; //$NON-NLS-1$
			failureCount = 0;
			bytesPerSecond = DownloadStatus.UNKNOWN_RATE;
		}

		/**
		 * Comparison used to sort mirrors.
		 */
		public int compareTo(Object o) {
			if (!(o instanceof MirrorInfo))
				return 0;
			MirrorInfo that = (MirrorInfo) o;
			//less failures is better
			if (this.failureCount != that.failureCount)
				return this.failureCount - that.failureCount;
			//faster is better
			if (this.bytesPerSecond != that.bytesPerSecond)
				return (int) (this.bytesPerSecond - that.failureCount);
			//trust that initial rank indicates geographical proximity
			return this.initialRank - that.initialRank;
		}

		public String toString() {
			return "Mirror(" + locationString + ',' + failureCount + ',' + bytesPerSecond + ')'; //$NON-NLS-1$
		}
	}

	/**
	 * The URI of the base repository being mirrored.
	 */
	URI baseURI;

	MirrorInfo[] mirrors;

	private final IRepository repository;

	private final Random random = new Random();

	/**
	 * Constructs a mirror support class for the given repository. Mirrors are
	 * not contacted and the mirrorsURL document is not parsed until a
	 * mirror location request is sent.
	 */
	public MirrorSelector(IRepository repository) {
		this.repository = repository;
		try {
			URL repositoryURL = repository.getLocation();
			if (repositoryURL != null)
				this.baseURI = URLUtil.toURI(repositoryURL);
		} catch (URISyntaxException e) {
			log("Error initializing mirrors for: " + repository.getLocation(), e); //$NON-NLS-1$
		}
	}

	/**
	 * Parses the given mirror URL to obtain the list of mirrors. Returns the mirrors,
	 * or null if mirrors could not be computed.
	 * 
	 * Originally copied from DefaultSiteParser.getMirrors in org.eclipse.update.core
	 */
	private MirrorInfo[] computeMirrors(String mirrorsURL) {
		try {
			String countryCode = Locale.getDefault().getCountry().toLowerCase();
			int timeZone = (new GregorianCalendar()).get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000);

			if (mirrorsURL.indexOf('?') != -1) {
				mirrorsURL = mirrorsURL + '&';
			} else {
				mirrorsURL = mirrorsURL + '?';
			}
			mirrorsURL = mirrorsURL + "countryCode=" + countryCode + "&timeZone=" + timeZone + "&responseType=xml"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document document = builder.parse(mirrorsURL);
			if (document == null)
				return null;
			NodeList mirrorNodes = document.getElementsByTagName("mirror"); //$NON-NLS-1$
			int mirrorCount = mirrorNodes.getLength();
			MirrorInfo[] infos = new MirrorInfo[mirrorCount + 1];
			for (int i = 0; i < mirrorCount; i++) {
				Element mirrorNode = (Element) mirrorNodes.item(i);
				String infoURL = mirrorNode.getAttribute("url"); //$NON-NLS-1$
				infos[i] = new MirrorInfo(infoURL, i);
			}
			//p2: add the base site as the last resort mirror so we can track download speed and failure rate
			infos[mirrorCount] = new MirrorInfo(repository.getLocation().toExternalForm(), mirrorCount);
			return infos;
		} catch (Exception e) {
			// log if absolute url
			if (mirrorsURL != null && (mirrorsURL.startsWith("http://") //$NON-NLS-1$
					|| mirrorsURL.startsWith("https://") //$NON-NLS-1$
					|| mirrorsURL.startsWith("file://") //$NON-NLS-1$
					|| mirrorsURL.startsWith("ftp://") //$NON-NLS-1$
			|| mirrorsURL.startsWith("jar://"))) //$NON-NLS-1$
				log("Error processing mirrors URL: " + mirrorsURL, e); //$NON-NLS-1$
			return null;
		}
	}

	/**
	 * Returns an equivalent location for the given artifact location in the base 
	 * repository.  Always falls back to the given input location in case of failure
	 * to compute mirrors. Never returns null.
	 */
	public synchronized String getMirrorLocation(String inputLocation) {
		Assert.isNotNull(inputLocation);
		if (baseURI == null)
			return inputLocation;
		URI relativeLocation = null;
		try {
			relativeLocation = baseURI.relativize(new URI(inputLocation));
		} catch (URISyntaxException e) {
			if (Tracing.DEBUG_MIRRORS)
				log("Unable to make location relative: " + inputLocation, e); //$NON-NLS-1$
		}
		//if we failed to relativize the location, we can't select a mirror
		if (relativeLocation == null || relativeLocation.isAbsolute())
			return inputLocation;
		MirrorInfo selectedMirror = selectMirror();
		if (selectedMirror == null)
			return inputLocation;
		if (Tracing.DEBUG_MIRRORS)
			Tracing.debug("Selected mirror for artifact " + inputLocation + ": " + selectedMirror); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			return new URL(selectedMirror.locationString + relativeLocation.getPath()).toExternalForm();
		} catch (MalformedURLException e) {
			log("Unable to make location " + inputLocation + " relative to mirror " + selectedMirror.locationString, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return inputLocation;
	}

	/**
	 * Returns the mirror locations for this repository, or <code>null</code> if
	 * they could not be computed.
	 */
	private MirrorInfo[] initMirrors() {
		if (mirrors != null)
			return mirrors;
		String mirrorsURL = (String) repository.getProperties().get(IRepository.PROP_MIRRORS_URL);
		if (mirrorsURL != null)
			mirrors = computeMirrors(mirrorsURL);
		return mirrors;
	}

	private void log(String message, Throwable exception) {
		LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message, exception));
	}

	/**
	 * Reports the result of a mirror download
	 */
	public synchronized void reportResult(String toDownload, IStatus result) {
		if (mirrors == null)
			return;
		for (int i = 0; i < mirrors.length; i++) {
			MirrorInfo mirror = mirrors[i];
			if (toDownload.startsWith(mirror.locationString)) {
				if (!result.isOK() && result.getSeverity() != IStatus.CANCEL)
					mirror.failureCount++;
				if (result instanceof DownloadStatus) {
					long oldRate = mirror.bytesPerSecond;
					long newRate = ((DownloadStatus) result).getTransferRate();
					//average old and new rate so one slow download doesn't ruin the mirror's reputation
					if (oldRate > 0)
						newRate = (oldRate + newRate) / 2;
					mirror.bytesPerSecond = newRate;
				}
				if (Tracing.DEBUG_MIRRORS)
					Tracing.debug("Updated mirror " + mirror); //$NON-NLS-1$
				Arrays.sort(mirrors);
				return;
			}
		}
	}

	/**
	 * Selects a mirror from the given list of mirrors. Returns null if a mirror
	 * could not be found.
	 */
	private MirrorInfo selectMirror() {
		initMirrors();
		int mirrorCount;
		if (mirrors == null || (mirrorCount = mirrors.length) == 0)
			return null;
		//this is a function that randomly selects a mirror based on a logarithmic
		//distribution. Mirror 0 has a 1/2 chance of being selected, mirror 1 has a 1/4 chance, 
		// mirror 2 has a 1/8 chance, etc. This introduces some variation in the mirror 
		//selection, while still favoring better mirrors
		int result = (int) (Math.log(random.nextInt(1 << Math.min(15, mirrorCount)) + 1) / LOG2);
		if (result >= mirrorCount)
			result = mirrorCount - 1;
		MirrorInfo selected = mirrors[mirrorCount - 1 - result];
		//if we selected a mirror that has failed in the past, revert to best available mirror
		if (selected.failureCount > 0)
			selected = mirrors[0];
		//for now, don't tolerate failing mirrors
		if (selected.failureCount > 0)
			return null;
		return selected;
	}

}
