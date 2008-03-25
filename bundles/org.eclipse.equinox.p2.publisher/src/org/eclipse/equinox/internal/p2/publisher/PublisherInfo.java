/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.osgi.framework.Version;

public class PublisherInfo implements IPublisherInfo {

	private int artifactOptions = 0;
	private IMetadataRepository metadataRepository;
	private IArtifactRepository artifactRepository;
	private String[] configurations;
	private List adviceList = new ArrayList(11);

	public void addAdvice(IPublishingAdvice advice) {
		adviceList.add(advice);
	}

	public Collection getAdvice(String configSpec, boolean includeDefault, String id, Version version, Class type) {
		ArrayList result = new ArrayList();
		for (Iterator i = adviceList.iterator(); i.hasNext();) {
			Object object = i.next();
			if (type.isInstance(object) && ((IPublishingAdvice) object).isApplicable(configSpec, includeDefault, id, version))
				result.add(object);
		}
		return result;
	}

	public IArtifactRepository getArtifactRepository() {
		return artifactRepository;
	}

	public IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	public int getArtifactOptions() {
		return artifactOptions;
	}

	public void setArtifactRepository(IArtifactRepository value) {
		artifactRepository = value;
	}

	public void setMetadataRepository(IMetadataRepository value) {
		metadataRepository = value;
	}

	public void setArtifactOptions(int value) {
		artifactOptions = value;
	}

	public String[] getConfigurations() {
		return configurations;
	}

	public void setConfigurations(String[] value) {
		configurations = value;
	}

	public String getSummary() {
		return "some text that describes this setup";
	}

}
