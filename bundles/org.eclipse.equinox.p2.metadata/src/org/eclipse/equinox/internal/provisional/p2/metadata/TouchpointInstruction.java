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
package org.eclipse.equinox.internal.provisional.p2.metadata;

public class TouchpointInstruction {

	private final String body;
	private final String importAttribute;

	TouchpointInstruction(String body) {
		this(body, null);
	}

	TouchpointInstruction(String body, String importAttribute) {
		this.body = body;
		this.importAttribute = importAttribute;
	}

	public String getBody() {
		return body;
	}

	public String getImportAttribute() {
		return importAttribute;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((importAttribute == null) ? 0 : importAttribute.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TouchpointInstruction other = (TouchpointInstruction) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (importAttribute == null) {
			if (other.importAttribute != null)
				return false;
		} else if (!importAttribute.equals(other.importAttribute))
			return false;
		return true;
	}
}
