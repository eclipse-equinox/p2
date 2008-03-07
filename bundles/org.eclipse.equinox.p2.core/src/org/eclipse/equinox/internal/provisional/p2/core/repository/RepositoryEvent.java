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
package org.eclipse.equinox.internal.provisional.p2.core.repository;

import java.net.URL;
import java.util.EventObject;

/**
 * An event indicating a repository was added, removed, or changed.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class RepositoryEvent extends EventObject {
	private static final long serialVersionUID = 3082402920617281765L;

	public static final int ADDED = 0;
	public static final int REMOVED = 1;
	public static final int CHANGED = 2;

	private final int kind, type;

	/**
	 * Creates a new repository event.
	 * 
	 * @param location the location of the repository that changed.
	 * @param repositoryType the type of repository that was changed
	 * @param kind the kind of change that occurred.
	 */
	public RepositoryEvent(URL location, int repositoryType, int kind) {
		super(location);
		this.kind = kind;
		this.type = repositoryType;

	}

	/**
	 * Returns the kind of change that occurred.
	 *
	 * @return the kind of change that occurred.
	 * @see #ADDED
	 * @see #REMOVED
	 * @see #CHANGED
	 */
	public int getKind() {
		return kind;
	}

	/**
	 * Returns the location of the repository that changed.
	 * 
	 * @return the location of the repository that changed.
	 */
	public URL getRepositoryLocation() {
		return (URL) getSource();
	}

	/**
	 * Returns the type of repository that was changed.
	 * 
	 * @return the type of repository that was changed
	 *  ({@link IRepository#TYPE_METADATA} or {@link IRepository#TYPE_ARTIFACT}).
	 */
	public int getRepositoryType() {
		return type;
	}

}
