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
 * An event indicating a repository was added, removed, changed,
 * or discovered.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class RepositoryEvent extends EventObject {
	private static final long serialVersionUID = 3082402920617281765L;

	/**
	 * A change kind constant (value 0), indicating a repository was added to the 
	 * list of repositories known to a repository manager.
	 */
	public static final int ADDED = 0;

	/**
	 * A change kind constant (value 1), indicating a repository was removed from 
	 * the list of repositories known to a repository manager.
	 */
	public static final int REMOVED = 1;

	/**
	 * A change kind constant (value 2), indicating a repository known to a 
	 * repository manager was modified.
	 */
	public static final int CHANGED = 2;

	/**
	 * A change kind constant (value 4), indicating a new repository was discovered.
	 * This event is a way to notify repository managers in a generic way about
	 * a newly discovered repository. The repository manager will typically receive
	 * this event, add the repository to its list of known repositories, and issue
	 * a subsequent {@link #ADDED} event. Other clients should not typically
	 * listen for this kind of event.
	 */
	public static final int DISCOVERED = 4;

	private final int kind, type;

	private boolean isEnabled;

	/**
	 * Creates a new repository event.
	 * 
	 * @param location the location of the repository that changed.
	 * @param repositoryType the type of repository that was changed
	 * @param kind the kind of change that occurred.
	 * @param enabled whether the repository is enabled
	 */
	public RepositoryEvent(URL location, int repositoryType, int kind, boolean enabled) {
		super(location);
		this.kind = kind;
		this.type = repositoryType;
		isEnabled = enabled;
	}

	/**
	 * Returns the kind of change that occurred.
	 *
	 * @return the kind of change that occurred.
	 * @see #ADDED
	 * @see #REMOVED
	 * @see #CHANGED
	 * @see #DISCOVERED
	 */
	public int getKind() {
		return kind;
	}

	/**
	 * Returns the location of the repository associated with this event.
	 * 
	 * @return the location of the repository associated with this event.
	 */
	public URL getRepositoryLocation() {
		return (URL) getSource();
	}

	/**
	 * Returns the type of repository associated with this event. Clients
	 * should not assume that the set of possible repository types is closed;
	 * clients should ignore events from repository types they don't know about.
	 * 
	 * @return the type of repository associated with this event.
	 *  ({@link IRepository#TYPE_METADATA} or {@link IRepository#TYPE_ARTIFACT}).
	 */
	public int getRepositoryType() {
		return type;
	}

	/**
	 * Returns whether the affected repository is enabled.
	 * 
	 * @return <code>true</code> if the repository is enabled,
	 * and <code>false</code> otherwise.
	 */
	public boolean isRepositoryEnabled() {
		return isEnabled;
	}

}
