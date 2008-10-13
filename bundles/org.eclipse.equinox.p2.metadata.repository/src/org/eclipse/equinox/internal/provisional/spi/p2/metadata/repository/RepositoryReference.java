/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository;

import java.net.URI;

/**
 * Serialization helper class for repository references.
 */
public class RepositoryReference {
	public URI Location;
	public int Type;
	public int Options;

	public RepositoryReference(URI location, int type, int options) {
		this.Location = location;
		this.Type = type;
		this.Options = options;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepositoryReference other = (RepositoryReference) obj;
		if (Location == null) {
			if (other.Location != null)
				return false;
		} else if (!Location.equals(other.Location))
			return false;
		if (Type != other.Type)
			return false;
		return true;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Location == null) ? 0 : Location.hashCode());
		result = prime * result + Type;
		return result;
	}

}