/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - public API
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.spi;

import org.eclipse.equinox.p2.repository.IRepositoryReference;

import java.net.URI;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * Serialization helper class for repository references.
 * @since 2.0
 */
public class RepositoryReference implements IRepositoryReference {
	private final URI location;
	private final int type;
	private final int options;
	private final String nickname;

	/**
	 * Creates a reference to another repository.
	 *
	 * The {@link IRepository#ENABLED} option flag controls whether the 
	 * referenced repository should be marked as enabled when added to the repository
	 * manager. If this flag is set, the repository will be marked as enabled when
	 * added to the repository manager. If this flag is missing, the repository will
	 * be marked as disabled.
	 * 
	 * @param location the location of the repository to add
	 * @param nickname The nickname of the repository, or <code>null</code>
	 * @param type the repository type (currently either {@link IRepository#TYPE_METADATA}
	 * or {@link IRepository#TYPE_ARTIFACT}).
	 * @param options bit-wise or of option constants (currently either 
	 * {@link IRepository#ENABLED} or {@link IRepository#NONE}).
	 * @see IMetadataRepositoryManager#setEnabled(URI, boolean)
	 */
	public RepositoryReference(URI location, String nickname, int type, int options) {
		this.location = location;
		this.type = type;
		this.options = options;
		this.nickname = nickname;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof IRepositoryReference))
			return false;
		IRepositoryReference other = (IRepositoryReference) obj;
		if (location == null) {
			if (other.getLocation() != null)
				return false;
		} else if (!location.equals(other.getLocation()))
			return false;
		if (type != other.getType())
			return false;
		return true;
	}

	public URI getLocation() {
		return location;
	}

	public int getType() {
		return type;
	}

	public int getOptions() {
		return options;
	}

	public String getNickname() {
		return nickname;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + type;
		return result;
	}

}