/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.EventObject;

public class ProfileEvent extends EventObject {
	private static final long serialVersionUID = 3082402920617281765L;

	public static byte ADDED = 0;
	public static byte REMOVED = 1;
	// TODO We need an event when profile properties change.  Not fully implemented.
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=197701
	public static byte CHANGED = 2;

	private byte reason;

	public ProfileEvent(Profile source, byte reason) {
		super(source);
		this.reason = reason;
	}

	public byte getReason() {
		return reason;
	}

	public Profile getProfile() {
		return (Profile) getSource();
	}

}
