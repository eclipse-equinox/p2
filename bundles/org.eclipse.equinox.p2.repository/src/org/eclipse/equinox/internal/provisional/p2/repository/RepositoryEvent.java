/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.repository;

import java.net.URI;
import java.util.EventObject;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.repository.IRepository;

/**
 * An event indicating a repository was added, removed, changed,
 * or discovered.
 * 
 * @see IProvisioningEventBus
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

	/**
	 * A change kind constant (value 8), indicating the repository's enablement
	 * was changed.  The {{@link #isRepositoryEnabled()} method can be used
	 * to find out the new enablement state of the repository, and to deduce
	 * the previous enablement state.
	 */
	public static final int ENABLEMENT = 8;

	private final int kind, type;
	private boolean isEnabled;
	private String nickname;

	/**
	 * Creates and returns a new repository discovery event.
	 * @param location the location of the repository that changed.
	 * @param nickname the repository nickname
	 * @param repositoryType the type of repository that was changed
	 * @param enabled whether the repository is enabled
	 * @return A new repository discovery event
	 * @see IRepository#PROP_NICKNAME
	 */
	public static RepositoryEvent newDiscoveryEvent(URI location, String nickname, int repositoryType, boolean enabled) {
		RepositoryEvent event = new RepositoryEvent(location, repositoryType, DISCOVERED, enabled);
		event.nickname = nickname;
		return event;
	}

	/**
	 * Creates a new repository event.
	 * 
	 * @param location the location of the repository that changed.
	 * @param repositoryType the type of repository that was changed
	 * @param kind the kind of change that occurred.
	 * @param enabled whether the repository is enabled
	 */
	public RepositoryEvent(URI location, int repositoryType, int kind, boolean enabled) {
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
	 * @see #ENABLEMENT
	 */
	public int getKind() {
		return kind;
	}

	/**
	 * Returns the nickname of the repository. This method is only applicable
	 * for the {@link #DISCOVERED} event type. For other event types this
	 * method returns <code>null</code>.
	 */
	public String getRepositoryNickname() {
		return nickname;
	}

	/**
	 * Returns the location of the repository associated with this event.
	 * 
	 * @return the location of the repository associated with this event.
	 */
	public URI getRepositoryLocation() {
		return (URI) getSource();
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

	/*
	 * (non-Javadoc)
	 * @see java.util.EventObject#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("RepositoryEvent["); //$NON-NLS-1$
		switch (kind) {
			case ADDED :
				buffer.append("ADDED "); //$NON-NLS-1$
				break;
			case CHANGED :
				buffer.append("CHANGED "); //$NON-NLS-1$
				break;
			case DISCOVERED :
				buffer.append("DISCOVERED "); //$NON-NLS-1$
				break;
			case ENABLEMENT :
				buffer.append("ENABLED "); //$NON-NLS-1$
				break;
			case REMOVED :
				buffer.append("REMOVED "); //$NON-NLS-1$
				break;
		}
		if (type == IRepository.TYPE_ARTIFACT)
			buffer.append("Artifact "); //$NON-NLS-1$
		else
			buffer.append("Metadata "); //$NON-NLS-1$
		// uri
		buffer.append(getSource().toString());
		if (nickname != null)
			buffer.append("Nickname: " + nickname); //$NON-NLS-1$
		buffer.append(" Enabled: " + Boolean.toString(isEnabled)); //$NON-NLS-1$
		buffer.append("] "); //$NON-NLS-1$
		return buffer.toString();
	}
}
