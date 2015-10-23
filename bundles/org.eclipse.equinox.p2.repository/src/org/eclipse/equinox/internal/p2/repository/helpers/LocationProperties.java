/******************************************************************************* 
* Copyright (c) 2010, 2015 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.internal.p2.repository.helpers;

import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * A representation of Location Properties.  Location properties are specified at a repository
 * location in an index.p2 file as a series of properties.  To create this object, use the
 * {@link LocationProperties#create(InputStream)} method. This method is guaranteed never to
 * return null, however, if the location properties file does not exist, or it cannot be read,
 * or it is improperly formated, isValid will return {@link LocationProperties#exists()} will 
 * return false.
 * 
 * Version 1 specifies the following 2 properties
 * version=1                                 <-- Required
 * metadata.repository.factory.order =       <-- Optional
 * artifact.repository.factory.order =       <-- Optional
 * md5.hash.<factoryID> =                    <-- Optional
 * 
 * Where repository.factory.order is a comma separated list of strings 
 * representing repository suffix filters.
 * 
 */
public class LocationProperties {

	public static final String END = "!"; //$NON-NLS-1$

	private static final String VERSION = "version"; //$NON-NLS-1$
	private static final String METADATA_REPOSITORY_FACTORY_ORDER = "metadata.repository.factory.order"; //$NON-NLS-1$
	private static final String ARTIFACT_REPOSITORY_FACTORY_ORDER = "artifact.repository.factory.order"; //$NON-NLS-1$
	private static final String MD5_HASH = "md5.hash."; //$NON-NLS-1$

	private boolean isValid = false;
	private Version version = Version.createOSGi(0, 0, 0); // Version 1
	private String[] metadataSearchOrder = new String[0]; // Version 1
	private String[] artifactSearchOrder = new String[0]; // Version 1
	private Map<String, Boolean> md5Hashes = null; // Version 1

	public static LocationProperties createEmptyIndexFile() {
		return new LocationProperties();
	}

	/**
	 * Creates a LocationProperties Object from an input stream. If the LocationProperties
	 * could be created, it is returned.  If it could not be created, an empty LocationProperties
	 * object is returned.  To check if the location properties file exists, call {@link LocationProperties#exists()};
	 * 
	 * @param stream The input stream from which to read the properties from
	 * @return LocationPropreties if the stream represents a valid set of Location Properties
	 */
	public static LocationProperties create(InputStream stream) {
		if (stream == null) {
			return new LocationProperties();
		}

		Properties properties = new Properties();
		try {
			properties.load(stream);
		} catch (Throwable e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
			return new LocationProperties();
		}

		if (properties.getProperty(VERSION) == null || properties.getProperty(VERSION).length() == 0)
			return new LocationProperties();

		try {
			Version version = Version.parseVersion(properties.getProperty(VERSION));
			if (version.compareTo(Version.createOSGi(1, 0, 0)) < 0)
				return new LocationProperties();

			LocationProperties locationProperties = new LocationProperties();
			if (version.compareTo(Version.createOSGi(1, 0, 0)) == 0) {
				if (locationProperties.initVersion1(properties))
					return locationProperties;
			}
		} catch (Throwable t) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, t.getMessage(), t));
		}
		return new LocationProperties();
	}

	private LocationProperties() {
		// empty constructor
	}

	/**
	 * Returns true if the location properties exist, could be read, and conforms to a proper version.  
	 * Returns false otherwise.
	 */
	public boolean exists() {
		return isValid;
	}

	/**
	 * Returns the Version of the location properties file.  This method is guaranteed 
	 * to return a value for all location property files >= to Version 1.0
	 * 
	 * @return The Version of this set of location properties
	 */
	public Version getVersion() {
		return this.version;
	}

	/**
	 * Returns the metadata FactoryID search order of location properties file.  This method is
	 * guaranteed to return a value for all location property files >= to Version 1.0
	 * For all other Versions, this method will return an empty string array
	 * 
	 * If END (!) is specified, then all other repositories should be ignored.
	 * 
	 * @return The Metadata FactoryID Search Order
	 */
	public String[] getMetadataFactorySearchOrder() {
		return this.metadataSearchOrder;
	}

	/**
	 * Returns the artifact FactoryID search order of location properties file.  This method is
	 * guaranteed to return a value for all location property files >= to Version 1.0
	 * For all other Versions, this method will return an empty string array
	 * 
	 * If END (!) is specified, then all other repositories should be ignored.
	 * 
	 * @return The Metadata FactoryID Search Order
	 */
	public String[] getArtifactFactorySearchOrder() {
		return this.artifactSearchOrder;
	}

	/**
	 * Returns true if an MD5 has exists for the specified factoryID.  If the specified
	 * factoryID was not specified in the LocatoinProperty file, this method
	 * will return false.
	 * 
	 * @param factoryID
	 */
	public boolean hasMD5Hash(String factoryID) {
		Boolean result = md5Hashes.get("md5." + factoryID); //$NON-NLS-1$
		if (result == null)
			return false;
		return result.booleanValue();
	}

	/*
	 * Sets up a set of location properties using the Version 1 format.
	 */
	private boolean initVersion1(Properties properties) {
		if (properties.get(VERSION) == null)
			return false;

		Set<Entry<Object, Object>> entrySet = properties.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			if (VERSION.equals(entry.getKey())) {
				this.version = Version.parseVersion((String) entry.getValue());
			} else if (METADATA_REPOSITORY_FACTORY_ORDER.equals(entry.getKey())) {
				this.metadataSearchOrder = getRepositoryFactoryOrder((String) entry.getValue());
			} else if (ARTIFACT_REPOSITORY_FACTORY_ORDER.equals(entry.getKey())) {
				this.artifactSearchOrder = getRepositoryFactoryOrder((String) entry.getValue());
			} else if (((String) entry.getKey()).startsWith(MD5_HASH)) {
				initHashMD5Hash((String) entry.getKey(), (String) entry.getValue());
			}
		}
		this.isValid = true;
		return true;
	}

	/**
	 * @param key 
	 * @param value
	 */
	private void initHashMD5Hash(String key, String value) {
		// Empty for now
	}

	private String[] getRepositoryFactoryOrder(String repositoryFactoryOrder) {
		repositoryFactoryOrder = repositoryFactoryOrder == null ? "" : repositoryFactoryOrder; //$NON-NLS-1$
		StringTokenizer tokenizer = new StringTokenizer(repositoryFactoryOrder, ","); //$NON-NLS-1$
		List<String> searchOrder = new ArrayList<String>();
		while (tokenizer.hasMoreTokens()) {
			searchOrder.add(tokenizer.nextToken().trim());
		}
		return searchOrder.toArray(new String[searchOrder.size()]);
	}
}
