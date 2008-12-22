/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui;

import java.util.EventObject;

/**
 *
 * Event used to signify that a batch change is about
 * to begin.  We should ignore listeners until it is
 * done.
 * 
 * @since 3.4
 */
public class BatchChangeBeginningEvent extends EventObject {

	private static final long serialVersionUID = -7529156836242774280L;

	/**
	 * Construct a new instance of this event.
	 * @param source the source of the event
	 */
	public BatchChangeBeginningEvent(Object source) {
		super(source);
	}

}
