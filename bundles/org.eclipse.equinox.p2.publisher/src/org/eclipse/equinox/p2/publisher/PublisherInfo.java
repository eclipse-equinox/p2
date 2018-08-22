/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
	private List<IPublisherAdvice> adviceList = new ArrayList<>(11);

	@Override
	public void addAdvice(IPublisherAdvice advice) {
		adviceList.add(advice);
	}

	public List<IPublisherAdvice> getAdvice() {
		return adviceList;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IPublisherAdvice> Collection<T> getAdvice(String configSpec, boolean includeDefault, String id, Version version, Class<T> type) {
		ArrayList<T> result = new ArrayList<>();
		for (IPublisherAdvice advice : adviceList) {
			if (type.isInstance(advice) && advice.isApplicable(configSpec, includeDefault, id, version))
				// Ideally, we would use Class.cast here but it was introduced in Java 1.5
				result.add((T) advice);
		}
		return result;
	}

	@Override
	public IArtifactRepository getArtifactRepository() {
		return artifactRepository;
	}

	@Override
	public IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	@Override
	public IArtifactRepository getContextArtifactRepository() {
		return contextArtifactRepository;
	}

	@Override
	public IMetadataRepository getContextMetadataRepository() {
		return contextMetadataRepository;
	}

	@Override
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

	@Override
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
