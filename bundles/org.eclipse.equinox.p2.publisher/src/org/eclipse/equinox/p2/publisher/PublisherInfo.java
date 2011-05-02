/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import java.util.*;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

public class PublisherInfo implements IPublisherInfo {

	private int artifactOptions = 0;
	private IMetadataRepository metadataRepository;
	private IArtifactRepository artifactRepository;
	private IMetadataRepository contextMetadataRepository;
	private IArtifactRepository contextArtifactRepository;
	private String[] configurations = new String[0];
	private List<IPublisherAdvice> adviceList = new ArrayList<IPublisherAdvice>(11);

	public void addAdvice(IPublisherAdvice advice) {
		adviceList.add(advice);
	}

	public List<IPublisherAdvice> getAdvice() {
		return adviceList;
	}

	@SuppressWarnings("unchecked")
	public <T extends IPublisherAdvice> Collection<T> getAdvice(String configSpec, boolean includeDefault, String id, Version version, Class<T> type) {
		ArrayList<T> result = new ArrayList<T>();
		for (IPublisherAdvice advice : adviceList) {
			if (type.isInstance(advice) && advice.isApplicable(configSpec, includeDefault, id, version))
				// Ideally, we would use Class.cast here but it was introduced in Java 1.5
				result.add((T) advice);
		}
		return result;
	}

	public IArtifactRepository getArtifactRepository() {
		return artifactRepository;
	}

	public IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	public IArtifactRepository getContextArtifactRepository() {
		return contextArtifactRepository;
	}

	public IMetadataRepository getContextMetadataRepository() {
		return contextMetadataRepository;
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

	public void setContextArtifactRepository(IArtifactRepository value) {
		contextArtifactRepository = value;
	}

	public void setContextMetadataRepository(IMetadataRepository value) {
		contextMetadataRepository = value;
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
		return "."; //$NON-NLS-1$
	}

}
