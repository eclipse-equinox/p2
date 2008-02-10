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
package org.eclipse.equinox.internal.p2.update;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * @since 1.0
 */
public class Configuration {

	private List sites = new ArrayList();
	String date;
	boolean transientProperty;
	String version;
	String shared_ur;

	public static Configuration load(File location) throws ProvisionException {
		return ConfigurationParser.parse(location);
	}

	public Configuration() {
		super();
	}

	public void save(File location) throws ProvisionException {
		ConfigurationWriter.save(this, location);
	}

	public String getSharedUR() {
		return shared_ur;
	}

	public void setSharedUR(String value) {
		shared_ur = value;
	}

	public List getSites() {
		return sites;
	}

	public void add(Site site) {
		sites.add(site);
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setVersion(String value) {
		version = value;
	}

	public String getVersion() {
		return version;
	}

	public void setTransient(boolean value) {
		transientProperty = value;
	}

	public boolean isTransient() {
		return transientProperty;
	}
}
