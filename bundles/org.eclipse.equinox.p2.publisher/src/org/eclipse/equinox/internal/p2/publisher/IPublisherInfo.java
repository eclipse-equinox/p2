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

import java.util.Collection;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;

public interface IPublisherInfo {

	/**
	 * A bitwise flag to say whether or not the index for the artifact repository should
	 * be updated.
	 */
	public static final int A_INDEX = 1;

	/**
	 * A bitwise flag to say whether or the artifacts themselves should be published.
	 */
	public static final int A_PUBLISH = 2;

	/**
	 * A bitwise flag to say whether or not to overwrite disk content discovered
	 * in the repository when publishing an artifact
	 */
	public static final int A_OVERWRITE = 4;

	/**
	 * Returns the artifact repository into which any publishable artifacts are published
	 * or <code>null</code> if none.
	 * @return a destination artifact repository or <code>null</code>
	 */
	public IArtifactRepository getArtifactRepository();

	/**
	 * Returns the metadata repository into which any publishable metadata is published
	 * or <code>null</code> if none.
	 * @return a destination metadata repository or <code>null</code>
	 */
	public IMetadataRepository getMetadataRepository();

	/**
	 * Returns whether or not artifacts themselves should be published.
	 * @return <code>true</code> if artifacts should be published.  
	 * <code>false</code> otherwise.
	 */
	public int getArtifactOptions();

	public IPublishingAdvice getAdvice(String id);

	public void setAdvice(String id, IPublishingAdvice value);

	public Collection getAdviceIds();

}
