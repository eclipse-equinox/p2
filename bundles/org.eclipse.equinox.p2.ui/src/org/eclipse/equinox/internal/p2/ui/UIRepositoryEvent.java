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

import java.net.URI;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;

/**
 * UIMetadataRepositoryEvent is used to distinguish those metadata repository
 * events of concern to the end user from those that are internal.
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=236485
 * 
 * @since 3.5
 *
 */
public class UIRepositoryEvent extends RepositoryEvent {

	private static final long serialVersionUID = 820293103398960019L;

	/**
	 * @param location
	 */
	public UIRepositoryEvent(URI location, int type, int kind) {
		super(location, type, kind, true);
	}

}
