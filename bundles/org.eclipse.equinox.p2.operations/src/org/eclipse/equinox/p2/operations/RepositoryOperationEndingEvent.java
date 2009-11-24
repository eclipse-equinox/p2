/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.operations;

import java.util.EventObject;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;

/**
 * Event used to signal that a repository operation has completed.  
 * 
 * @since 2.0
 */
public class RepositoryOperationEndingEvent extends EventObject {

	private static final long serialVersionUID = -4513769756968621852L;

	/**
	 * A repository event describing the nature of the operation.  
	 */
	private RepositoryEvent event;

	/**
	 * A boolean indicating whether the entire operation event should
	 * be suppressed.
	 */
	private boolean suppress;

	/**
	 * Construct a new instance of this event.
	 * 
	 * @param source the source of the event
	 * 
	 * @param item the object that was involved in the operation.  May be
	 * <code>null</code>.  This item may be used by clients to determine
	 * what should be updated after an operation completes.
	 */
	public RepositoryOperationEndingEvent(Object source, boolean suppress, RepositoryEvent event) {
		super(source);
		this.suppress = suppress;
		this.event = event;
	}

	/**
	 * Return a {@link RepositoryEvent} that reflects the operation that
	 * occurred.  A <code>null</code> return value indicates that there
	 * was not a single underlying repository operation.
	 * 
	 * @return the {@link RepositoryEvent} that was involved in the operation.  May be
	 * <code>null</code>.  This event may be used by clients to determine
	 * what should be updated after an operation completes.

	 */
	public RepositoryEvent getEvent() {
		return event;
	}

	/**
	 * Return a boolean that indicates whether the client should respond to this
	 * event.
	 * 
	 * @return <code>true</code> if clients should respond to this event, <code>false</code>
	 * if the client should ignore the entire operation.
	 */
	public boolean ignoreEvent() {
		return suppress;
	}

}
