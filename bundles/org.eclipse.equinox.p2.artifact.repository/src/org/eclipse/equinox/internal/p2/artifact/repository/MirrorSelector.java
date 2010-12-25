/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - bug fixes
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.repository.DownloadStatus;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.repository.IRepository;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

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
	public static class MirrorInfo implements Comparable<MirrorInfo> {
		private static final long PRIMARY_FAILURE_LINGER_TIME = 30000; // Retry again after 30 seconds
		private static final long SECONDARY_FAILURE_LINGER_TIME = 300000; // Wait 5 minutes
		private static final int ACCEPTABLE_FILE_NOT_FOUND_COUNT = 5; // Given an established connection, those are generally quick
		private static final Timer resetFailure = new Timer(true);

		long bytesPerSecond;
		int failureCount;
		int fileNotFoundCount;
		int totalFailureCount;
		private final int initialRank;
		String locationString;

		public MirrorInfo(String location, int initialRank) {
			this.initialRank = initialRank;
			this.locationString = location;
			if (!locationString.endsWith("/")) //$NON-NLS-1$
				locationString = locationString + "/"; //$NON-NLS-1$
			failureCount = 0;
			totalFailureCount = 0;
			bytesPerSecond = DownloadStatus.UNKNOWN_RATE;
		}

		/**
		 * Comparison used to sort mirrors.
		 */
		public synchronized int compareTo(MirrorInfo that) {
			synchronized (that) {
				double rank = 0.0;
				if (bytesPerSecond != that.bytesPerSecond) {
					if (bytesPerSecond != DownloadStatus.UNKNOWN_RATE && that.bytesPerSecond != DownloadStatus.UNKNOWN_RATE) {
						if (bytesPerSecond > that.bytesPerSecond)
							rank -= (double) bytesPerSecond / (double) that.bytesPerSecond;
						else
							rank += (double) that.bytesPerSecond / (double) bytesPerSecond;
					}
				}

				//less failures is better
				if (failureCount != that.failureCount) {
					if (failureCount > that.failureCount)
						rank += ((double) (failureCount + 1) / (double) (that.failureCount + 1)) * 2.0;
					else
						rank -= ((double) (that.failureCount + 1) / (double) (failureCount + 1)) * 2.0;
				}

				if (rank == 0.0)
					//trust that initial rank indicates geographical proximity
					rank = initialRank - that.initialRank;

				if (rank == 0.0)
					return 0;

				int intRank;
				intRank = (int) rank;
				if (intRank == 0)
					intRank = rank > 0 ? 1 : -1;
				return intRank;
			}
		}

		public synchronized String toString() {
			return "Mirror(" + locationString + ',' + failureCount + ',' + bytesPerSecond + ')'; //$NON-NLS-1$
		}

		public synchronized void decrementFailureCount() {
			if (failureCount > 0)
				failureCount--;
		}

		public synchronized void incrementFailureCount() {
			++failureCount;
			++totalFailureCount;
			if (totalFailureCount < 3) {
				resetFailure.schedule(new TimerTask() {
					@Override
					public void run() {
						decrementFailureCount();
					}
				}, totalFailureCount == 1 ? PRIMARY_FAILURE_LINGER_TIME : SECONDARY_FAILURE_LINGER_TIME);
			}
		}

		public synchronized void setBytesPerSecond(long newValue) {
			// Any non-positive value is treated as an unknown rate
			if (newValue <= 0)
				newValue = DownloadStatus.UNKNOWN_RATE;

			if (newValue > 0)
				// Back in commission
				failureCount = 0;
			bytesPerSecond = newValue;
		}

		public synchronized void incrementFileNotFoundCount() {
			if (++fileNotFoundCount > ACCEPTABLE_FILE_NOT_FOUND_COUNT) {
				incrementFailureCount();
				fileNotFoundCount = 0;
			}
		}
	}

	/**
	 * The URI of the base repository being mirrored.
	 */
	URI baseURI;

	MirrorInfo[] mirrors;

	private final IRepository<?> repository;

	private final Random random = new Random();

	private final Transport transport;

	/**
	 * Constructs a mirror support class for the given repository. Mirrors are
	 * not contacted and the mirrorsURL document is not parsed until a
	 * mirror location request is sent.
	 */
	public MirrorSelector(IRepository<?> repository, Transport transport) {
		this.repository = repository;
		this.transport = transport;
		try {
			String base = repository.getProperties().get(IRepository.PROP_MIRRORS_BASE_URL);
			if (base != null) {
				this.baseURI = new URI(base);
			} else {
				URI repositoryLocation = repository.getLocation();
				if (repositoryLocation != null)
					this.baseURI = repositoryLocation;
			}
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
	private MirrorInfo[] computeMirrors(String mirrorsURL, IProgressMonitor monitor) {
		try {
			String countryCode = Activator.getContext().getProperty("eclipse.p2.countryCode"); //$NON-NLS-1$
			if (countryCode == null || countryCode.trim().length() == 0)
				countryCode = Locale.getDefault().getCountry().toLowerCase();
			String timeZone = Activator.getContext().getProperty("eclipse.p2.timeZone"); //$NON-NLS-1$
			if (timeZone == null || timeZone.trim().length() == 0)
				timeZone = Integer.toString(new GregorianCalendar().get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000));

			if (mirrorsURL.indexOf('?') != -1) {
				mirrorsURL = mirrorsURL + '&';
			} else {
				mirrorsURL = mirrorsURL + '?';
			}
			mirrorsURL = mirrorsURL + "countryCode=" + countryCode + "&timeZone=" + timeZone + "&format=xml"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document document = null;
			// Use Transport to read the mirrors list (to benefit from proxy support, authentication, etc)
			InputSource input = new InputSource(mirrorsURL);
			input.setByteStream(transport.stream(URIUtil.fromString(mirrorsURL), monitor));
			document = builder.parse(input);
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
			infos[mirrorCount] = new MirrorInfo(baseURI.toString(), mirrorCount);
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
	public synchronized URI getMirrorLocation(URI inputLocation, IProgressMonitor monitor) {
		Assert.isNotNull(inputLocation);
		if (baseURI == null)
			return inputLocation;
		URI relativeLocation = baseURI.relativize(inputLocation);
		//if we failed to relativize the location, we can't select a mirror
		if (relativeLocation == null || relativeLocation.isAbsolute())
			return inputLocation;
		MirrorInfo selectedMirror = selectMirror(monitor);
		if (selectedMirror == null)
			return inputLocation;
		if (Tracing.DEBUG_MIRRORS)
			Tracing.debug("Selected mirror for artifact " + inputLocation + ": " + selectedMirror); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			return new URI(selectedMirror.locationString + relativeLocation.getPath());
		} catch (URISyntaxException e) {
			log("Unable to make location " + inputLocation + " relative to mirror " + selectedMirror.locationString, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return inputLocation;
	}

	/**
	 * Returns the mirror locations for this repository, or <code>null</code> if
	 * they could not be computed.
	 */
	private MirrorInfo[] initMirrors(IProgressMonitor monitor) {
		if (mirrors != null)
			return mirrors;
		String mirrorsURL = repository.getProperties().get(IRepository.PROP_MIRRORS_URL);
		if (mirrorsURL != null)
			mirrors = computeMirrors(mirrorsURL, monitor);
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
				if (!result.isOK() && result.getSeverity() != IStatus.CANCEL) {
					// Punishing a mirror harshly for a FileNotFoundException can be very wrong.
					// Some artifacts are not found on any mirror. When that's the case,
					// the best mirrors will be the first to receive that kind of punishment.
					//
					if (result.getException() instanceof FileNotFoundException)
						mirror.incrementFileNotFoundCount();
					else
						mirror.incrementFailureCount();
				}
				if (result instanceof DownloadStatus) {
					long oldRate = mirror.bytesPerSecond;
					long newRate = ((DownloadStatus) result).getTransferRate();
					//average old and new rate so one slow download doesn't ruin the mirror's reputation
					if (oldRate > 0)
						newRate = (oldRate + newRate) / 2;
					mirror.setBytesPerSecond(newRate);
				}
				if (Tracing.DEBUG_MIRRORS)
					Tracing.debug("Updated mirror " + mirror); //$NON-NLS-1$
				return;
			}
		}
	}

	/** 
	 * Return whether or not all the mirrors for this selector have proven to be invalid
	 * @return whether or not there is a valid mirror in this selector.
	 */
	public synchronized boolean hasValidMirror() {
		// return true if there is a mirror and it doesn't have multiple failures.
		if (mirrors == null || mirrors.length == 0)
			return false;
		Arrays.sort(mirrors);
		return mirrors[0].failureCount < 2;
	}

	/**
	 * Selects a mirror from the given list of mirrors. Returns null if a mirror
	 * could not be found.
	 */
	private MirrorInfo selectMirror(IProgressMonitor monitor) {
		initMirrors(monitor);
		int mirrorCount;
		if (mirrors == null || (mirrorCount = mirrors.length) == 0)
			return null;

		MirrorInfo selected;
		if (mirrorCount == 1)
			selected = mirrors[0];
		else {
			Arrays.sort(mirrors);
			for (;;) {
				//this is a function that randomly selects a mirror based on a logarithmic
				//distribution. Mirror 0 has a 1/2 chance of being selected, mirror 1 has a 1/4 chance, 
				// mirror 2 has a 1/8 chance, etc. This introduces some variation in the mirror 
				//selection, while still heavily favoring better mirrors
				//the algorithm computes the most significant digit in a binary number by computing the base 2 logarithm
				//if the first digit is most significant, mirror 0 is selected, if the second is most significant, mirror 1 is selected, etc
				int highestMirror = Math.min(15, mirrorCount);
				int result = (int) (Math.log(random.nextInt(1 << highestMirror) + 1) / LOG2);
				if (result >= highestMirror || result < 0)
					result = highestMirror - 1;

				int mirrorIndex = highestMirror - 1 - result;
				selected = mirrors[mirrorIndex];

				// If the ranking of the selected mirror is significantly worse then the top ranked
				// mirror, then we consider this a bad choice and reiterate.
				if (mirrorIndex == 0 || mirrors[0].compareTo(selected) < 4)
					// This is good enough
					break;
			}
		}

		//for now, don't tolerate mirrors with multiple failures
		if (selected.failureCount > 1)
			return null;
		return selected;
	}

}
