/*******************************************************************************
 *  Copyright (c) 2008, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - public API
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.spi;

import java.net.URI;
import java.util.Objects;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * Concrete implementation of a repository reference. This class can be used
 * by clients to define new repository references.
 * @see IMetadataRepository#addReferences(java.util.Collection)
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
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

	@Override
	public int hashCode() {
		return Objects.hash(location, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof IRepositoryReference other //
				&& Objects.equals(location, other.getLocation()) //
				&& type == other.getType();
	}

	@Override
	public URI getLocation() {
		return location;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public int getOptions() {
		return options;
	}

	@Override
	public String getNickname() {
		return nickname;
	}

	@Override
	public String toString() {
		String status = ((options & IRepository.NONE) != 0) ? "enabled" : " disabled"; //$NON-NLS-1$//$NON-NLS-2$
		return "location=" + location + (nickname != null && !nickname.isBlank() ? " name=" + nickname : "") + " " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
				+ status;
	}

}