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
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.net.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.w3c.dom.*;

/**
 * Mirror support class for {@link SimpleArtifactRepository}. This class implements
 * mirror support equivalent to update manager. A mirror URL returns an XML document
 * containing a list of mirrors, sorted geographically with closer mirrors first.
 */
public class Mirrors {
	URI baseURI;
	URL[] locations;
	private final IArtifactRepository repository;

	/**
	 * Parses the given mirror URL to obtain the list of mirrors. Returns the mirrors,
	 * or null if mirrors could not be computed.
	 * 
	 * Originally copied from DefaultSiteParser.getMirrors in org.eclipse.update.core
	 */
	static URL[] computeMirrors(String mirrorsURL) {
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
			URL[] mirrors = new URL[mirrorNodes.getLength()];
			for (int i = 0; i < mirrorNodes.getLength(); i++) {
				Element mirrorNode = (Element) mirrorNodes.item(i);
				String infoURL = mirrorNode.getAttribute("url"); //$NON-NLS-1$
				mirrors[i] = new URL(infoURL);
			}
			return mirrors;
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

	private static void log(String message, Throwable exception) {
		LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message, exception));
	}

	public Mirrors(IArtifactRepository repository) {
		this.repository = repository;
		try {
			URL repositoryURL = repository.getLocation();
			this.baseURI = new URI(repositoryURL.toExternalForm());
		} catch (URISyntaxException e) {
			log("Error initializing mirrors for: " + repository.getLocation(), e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns an equivalent location for the given artifact location in the base 
	 * repository.  Always falls back to the given input location in case of failure
	 * to compute mirrors.
	 */
	public String getMirrorLocation(String inputLocation) {
		URI relativeLocation = null;
		try {
			relativeLocation = baseURI.relativize(new URI(inputLocation));
		} catch (URISyntaxException e) {
			log("Unable to make location relative: " + inputLocation, e); //$NON-NLS-1$
		}
		//if we failed to relativize the location, we can't select a mirror
		if (relativeLocation == null || relativeLocation.isAbsolute())
			return inputLocation;
		URL[] mirrorLocations = getMirrorLocations();
		if (mirrorLocations == null)
			return inputLocation;
		URL selectedMirror = selectMirror(mirrorLocations);
		if (selectedMirror == null)
			return inputLocation;
		try {
			return new URL(selectedMirror, relativeLocation.getPath()).toExternalForm();
		} catch (MalformedURLException e) {
			log("Unable to make location " + inputLocation + " relative to mirror " + selectedMirror.toExternalForm(), e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return inputLocation;
	}

	/**
	 * Returns the mirror locations for this repository, or <code>null</code> if
	 * they could not be computed.
	 */
	private URL[] getMirrorLocations() {
		if (locations != null)
			return locations;
		String mirrorsURL = (String) repository.getProperties().get(IRepository.PROP_MIRRORS_URL);
		if (mirrorsURL != null && baseURI != null)
			locations = computeMirrors(mirrorsURL);
		return locations;
	}

	/**
	 * Selects a mirror from the given list of mirrors
	 */
	private URL selectMirror(URL[] mirrorLocations) {
		//TODO mirror weighting and dynamic selection
		return mirrorLocations[0];
	}

}
