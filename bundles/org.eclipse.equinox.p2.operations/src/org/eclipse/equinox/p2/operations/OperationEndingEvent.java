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
 * Event used to signal that a provisioning operation has completed.  
 * 
 * @since 2.0
 */
public class OperationEndingEvent extends EventObject {

	private static final long serialVersionUID = -4513769756968621852L;

	/**
	 * The last item touched in the operation.  Can help indicate what should be updated.
	 */
	private Object item;

	/**
	 * Construct a new instance of this event.
	 * 
	 * @param source the source of the event
	 * 
	 * @item the object that was involved in the operation.  May be
	 * <code>null</code>.  This item may be used by clients to determine
	 * what should be updated after an operation completes.
	 */
	public OperationEndingEvent(Object source, Object item) {
		super(source);
		this.item = item;
	}

	/**
	 * Return the object that was involved in the operation that is completing.
	 * @return the object that was involved in the operation.  May be
	 * <code>null</code>.  This item may be used by clients to determine
	 * what should be updated after an operation completes.

	 */
	public Object getItem() {
		return item;
	}

}
