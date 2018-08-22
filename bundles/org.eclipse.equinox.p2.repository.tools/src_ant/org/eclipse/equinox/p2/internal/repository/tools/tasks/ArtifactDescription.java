/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.HashMap;
import java.util.Map;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Parameter;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.repository.artifact.*;

public class ArtifactDescription extends DataType {

	private String classifier = null;
	private String id = null;
	private String version = null;
	private String range = null;
	private Map<String, String> properties = null;

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setRange(String range) {
		this.range = range;
	}

	public void addConfiguredProperty(Parameter property) {
		if (properties == null)
			properties = new HashMap<>();

		properties.put(property.getName(), property.getValue());
	}

	public String getClassifier() {
		return classifier;
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}

	public IQuery<IArtifactKey> createKeyQuery() {
		VersionRange keyRange = null;
		if (range != null)
			keyRange = VersionRange.create(range);
		else if (version != null) {
			Version keyVersion = Version.parseVersion(version);
			keyRange = new VersionRange(keyVersion, true, keyVersion, true);
		}
		return new ArtifactKeyQuery(classifier, id, keyRange);
	}

	public IQuery<IArtifactDescriptor> createDescriptorQuery() {
		VersionRange keyRange = null;
		if (range != null)
			keyRange = VersionRange.create(range);
		else if (version != null) {
			Version keyVersion = Version.parseVersion(version);
			keyRange = new VersionRange(keyVersion, true, keyVersion, true);
		}
		return new ArtifactDescriptorQuery(id, keyRange, null, properties);
	}
}
