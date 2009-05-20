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

package org.eclipse.equinox.internal.p2.ui;

import java.util.EventObject;

/**
 * Internal UI event used to signify that a batch change in which
 * we were ignoring listeners is done.
 * 
 * @since 3.4
 */
public class BatchChangeCompleteEvent extends EventObject {

	private static final long serialVersionUID = -4513769756968621852L;

	/**
	 * When the batch event is received, do we treat it as notification
	 * or ignore it?
	 */
	public boolean notify;

	/**
	 * Construct a new instance of this event.
	 * @param source the source of the event
	 */
	public BatchChangeCompleteEvent(Object source, boolean notify) {
		super(source);
		this.notify = notify;
	}

}
