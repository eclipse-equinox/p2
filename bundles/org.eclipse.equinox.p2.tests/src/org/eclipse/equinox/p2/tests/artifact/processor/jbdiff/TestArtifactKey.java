/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processor.jbdiff;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.osgi.framework.Version;

public class TestArtifactKey implements IArtifactKey {

	private String namespace;
	private String classifier;
	private String id;
	private Version version;

	public TestArtifactKey(String namespace, String classifier, String id, Version version) {
		super();
		this.namespace = namespace;
		this.classifier = classifier;
		this.id = id;
		this.version = version;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getId() {
		return id;
	}

	public String getNamespace() {
		return namespace;
	}

	public Version getVersion() {
		return version;
	}

}
