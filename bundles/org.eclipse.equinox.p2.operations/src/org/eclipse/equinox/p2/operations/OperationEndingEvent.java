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

/**
 * Internal UI event used to signify that a batch change in which
 * we were ignoring listeners is done.
 * 
 * @since 3.4
 */
public class OperationEndingEvent extends EventObject {

	private static final long serialVersionUID = -4513769756968621852L;

	/**
	 * When the batch event is received, do we treat it as notification
	 * or ignore it?
	 */
	public boolean notify;

	/**
	 * The event that should be processed at the end of the batch
	 */
	private EventObject event;

	/**
	 * Construct a new instance of this event.
	 * @param source the source of the event
	 */
	public OperationEndingEvent(Object source, EventObject event, boolean notify) {
		super(source);
		this.notify = notify;
		this.event = event;
	}

	public EventObject getLastEvent() {
		return event;
	}

}
