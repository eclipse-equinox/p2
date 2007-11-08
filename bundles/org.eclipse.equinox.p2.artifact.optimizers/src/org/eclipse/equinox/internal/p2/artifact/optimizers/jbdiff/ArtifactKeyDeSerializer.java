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
package org.eclipse.equinox.internal.p2.artifact.optimizers.jbdiff;

import java.util.StringTokenizer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.osgi.framework.Version;

/**
 * The <code>ArtifactKeyDeSerializer</code> encapsulates the serialization and de-serialization
 * of artifact keys into string representation and vice versa.
 * This encoding pattern is used within the processing step descriptor´s data property.<p>
 * <b>Note: </b>This class is a duplicate of {@link org.eclipse.equinox.internal.p2.artifact.processor.jbdiff.ArtifactKeyDeSerializer}.
 * This has been done because this class is only relevant for the delta stuff.   
 */
public class ArtifactKeyDeSerializer {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String SEPARATOR = ","; //$NON-NLS-1$

	public static IArtifactKey deserialize(String data) {

		if (data == null || data.length() == 0)
			throw new IllegalArgumentException("Artifact key could not be deserialized. Null or empty. Serialized key is " + data); //$NON-NLS-1$

		String[] parts = new String[4];
		StringTokenizer tokenizer = new StringTokenizer(data, SEPARATOR, true);
		int i = 0;
		int sepCount = 0;
		boolean lastTokenWasSep = true;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (!token.equals(SEPARATOR)) {
				parts[i++] = token;
				lastTokenWasSep = false;
				continue;
			}
			sepCount++;
			if (lastTokenWasSep) {
				parts[i++] = EMPTY_STRING;
				continue;
			}
			lastTokenWasSep = true;
		}

		if (sepCount != 3)
			throw new IllegalArgumentException("Artifact key could not be deserialized. Unexpected number of parts. Serialized key is " + data); //$NON-NLS-1$

		if (i == 3)
			parts[i++] = EMPTY_STRING;

		try {
			return new ArtifactKey(parts[0], parts[1], parts[2], Version.parseVersion(parts[3]));
		} catch (IllegalArgumentException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("Artifact key could not be deserialized. Wrong version syntay. Serialized key is " + data).initCause(e); //$NON-NLS-1$
		}
	}

	public static String serialize(IArtifactKey key) {
		StringBuffer data = new StringBuffer(key.getNamespace()).append(SEPARATOR);
		data.append(key.getClassifier()).append(SEPARATOR);
		data.append(key.getId()).append(SEPARATOR);
		data.append(key.getVersion().toString());
		return data.toString();
	}

	private static class ArtifactKey implements IArtifactKey {

		private final String namespace;
		private final String id;
		private final String classifier;
		private final Version version;
		private static final char SEP_CHAR = ',';

		protected ArtifactKey(String namespace, String classifier, String id, Version version) {
			super();
			Assert.isNotNull(namespace);
			Assert.isNotNull(classifier);
			Assert.isNotNull(id);
			Assert.isNotNull(version);
			if (namespace.indexOf(SEP_CHAR) != -1)
				throw new IllegalArgumentException("comma not allowed in namespace"); //$NON-NLS-1$
			if (classifier.indexOf(SEP_CHAR) != -1)
				throw new IllegalArgumentException("comma not allowed in classifier"); //$NON-NLS-1$
			if (id.indexOf(SEP_CHAR) != -1)
				throw new IllegalArgumentException("comma not allowed in id"); //$NON-NLS-1$
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

}
