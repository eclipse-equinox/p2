/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;


/**
 * Provide standardised artifact information to uniquely identify the 
 * corresponding bytes (perhaps not stored as a file). 
 * <p>
 * Artifact keys represent both a unique opaque identifier as well as structured 
 * and standardised pieces of information.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IArtifactKey extends IVersionedId {

	/**
	 * Returns the classifier for this artifact key. The returned value can be empty.
	 * @return the classifier segment of the key.
	 */
	public String getClassifier();

	/**
	 * Returns the id for this artifact key.
	 * @return the id segment of the key.
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
