/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import org.osgi.framework.Version;

/**
 * Provide standardized artifact information to uniquely identify the 
 * corresponding bytes (perhaps not stored as a file). 
 * <p>
 * Artifact keys represent both a unique opaque identifier as well as structured 
 * and standardized pieces of information.
 */

public interface IArtifactKey {

	/**
	 * The namespace.
	 * @return This returns the namespace segment of the artifact. Never
	 *         null or empty.
	 */
	String getNamespace();

	/**
	 * The classifier.
	 * @return This returns the classifier segment of the key. Never
	 *         null. Can be empty.
	 */
	String getClassifier();

	/**
	 * The identity of the artifact.
	 * @return This returns the id segment of the artifact. Can
	 *         be empty.
	 *         
	 * TODO: consider renaming this to getIdentity.
	 */
	String getId();

	/**
	 * The version of the artifact.
	 * @return This returns the version of the artifact. Never null. Can
	 *         be empty (Version.emptyVersion).
	 */
	Version getVersion();
}
