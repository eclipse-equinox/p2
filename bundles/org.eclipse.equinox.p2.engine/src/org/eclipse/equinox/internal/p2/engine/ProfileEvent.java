/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.equinox.p2.engine.IProfileEvent;

import java.util.EventObject;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class ProfileEvent extends EventObject implements IProfileEvent {
	private static final long serialVersionUID = 3082402920617281765L;

	private byte reason;

	public ProfileEvent(String profileId, byte reason) {
		super(profileId);
		this.reason = reason;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfileEvent#getReason()
	 */
	public byte getReason() {
		return reason;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfileEvent#getProfileId()
	 */
	public String getProfileId() {
		return (String) getSource();
	}
}
