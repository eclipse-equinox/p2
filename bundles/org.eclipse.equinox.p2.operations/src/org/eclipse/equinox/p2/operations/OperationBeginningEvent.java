/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.operations;

import java.util.EventObject;

/**
 * Event used to signal that a provisioning operation is about
 * to begin.  This event can be used to ignore lower-level events
 * until the operation is complete.
 *  
 * @since 2.0
 */
public class OperationBeginningEvent extends EventObject {

	private static final long serialVersionUID = -7529156836242774280L;

	/**
	 * Construct a new instance of this event.
	 * @param source the source of the event
	 */
	public OperationBeginningEvent(Object source) {
		super(source);
	}

}
