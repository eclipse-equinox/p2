/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import org.apache.tools.ant.Task;

/**
 * @since 1.0
 */
public class IUTask extends Task {

	private String id;
	private String version;

	public IUTask() {
		super();
	}

	public void setId(String value) {
		this.id = value;
	}

	public void setVersion(String value) {
		this.version = value;
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}
}
