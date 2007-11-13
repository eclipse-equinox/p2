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
	 * Returns the namespace for this artifact key. The returned value can never be empty.
	 * @return the namespace segment of the key.
	 */
	public String getNamespace();

	/**
	 * Returns the classifier for this artifact key. The returned value can be empty.
	 * @return the classifier segment of the key.
	 */
	public String getClassifier();

	/**
	 * Returns the id for this artifact key. The returned value can be empty.
	 * @return the classifier segment of the key.
	 */
	public String getId();

	/**
	 * Returns the version for this artifact key. 
	 * @return the version segment of the key.
	 */
	public Version getVersion();

	/**
	 * Returns the canonical string form of this artifact key.
	 * @return the canonical string representing this key
	 */
	public String toExternalForm();
}
