/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.simpleconfigurator;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.simpleConfigurator.ConfigurationConstants;

public class ConfigurationReader {
	static String CONFIG_LOCATION = ConfigurationConstants.CONFIGURATOR_FOLDER + '/' + ConfigurationConstants.CONFIG_LIST;

	private URL configurationLocation;
	private long stateLastTimestamp;
	private long configLastTimestamp;

	public ConfigurationReader(URL location) {
		this.configurationLocation = location;
	}

	private void readTimestampFile() {
		DataInputStream stream = null;
		try {
			stream = new DataInputStream(new URL(configurationLocation, ConfigurationConstants.CONFIGURATOR_FOLDER + '/' + ConfigurationConstants.LAST_CONFIG_STAMP).openStream());
			configLastTimestamp = stream.readLong();
			stateLastTimestamp = stream.readLong();
		} catch (Exception e) {
			configLastTimestamp = -1;
			stateLastTimestamp = -1;
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e1) {
					e1.printStackTrace();
					//					Utils.log(e1.getLocalizedMessage());
				}
		}
	}

	private BundleInfo[] readList() {
		Collection bundles = new ArrayList();
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(new URL(configurationLocation, CONFIG_LOCATION).openStream()));
			String line;
			try {
				while ((line = r.readLine()) != null) {
					StringTokenizer tok = new StringTokenizer(line, ",");
					BundleInfo bundle = new BundleInfo();
					bundle.setSymbolicName(tok.nextToken());
					bundle.setVersion(tok.nextToken());
					bundle.setLocation(tok.nextToken());
					bundle.setStartLevel(Integer.parseInt(tok.nextToken().trim()));
					bundle.setExpectedState(Integer.parseInt(tok.nextToken().trim()));
					bundles.add(bundle);
				}
			} finally {
				try {
					r.close();
				} catch (IOException ex) {
					// ignore
				}
			}
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			//TODO log something
			return null;
		} catch (IOException e) {
			//e.printStackTrace();
			//TODO log something
			return null;
		}
		return (BundleInfo[]) bundles.toArray(new BundleInfo[bundles.size()]);
		//TODO Need to close the stream in the catch as well 
	}

	public BundleInfo[] getExpectedState() {
		return readList();
	}

}
