/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.engine;

import java.net.URL;
import java.util.*;

public class ProvisioningContext {
	private URL[] metadataRepositories;
	private Properties properties = new Properties();
	private List extraIUs = new ArrayList();

	public ProvisioningContext() {
		// null repos means look at them all
		metadataRepositories = null;
	}

	public ProvisioningContext(URL[] metadataRepositories) {
		this.metadataRepositories = metadataRepositories;
	}

	public URL[] getMetadataRepositories() {
		return metadataRepositories;
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	public List getExtraIUs() {
		return extraIUs;
	}

	public void setExtraIUs(List extraIUs) {
		this.extraIUs = extraIUs;
	}
}
