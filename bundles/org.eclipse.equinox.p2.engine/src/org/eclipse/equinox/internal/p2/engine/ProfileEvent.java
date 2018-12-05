/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.EventObject;
import org.eclipse.equinox.p2.engine.IProfileEvent;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class ProfileEvent extends EventObject implements IProfileEvent {
	private static final long serialVersionUID = 3082402920617281765L;

	private int reason;

	public ProfileEvent(String profileId, int reason) {
		super(profileId);
		this.reason = reason;
	}

	@Override
	public int getReason() {
		return reason;
	}

	@Override
	public String getProfileId() {
		return (String) getSource();
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("ProfileEvent["); //$NON-NLS-1$
		buffer.append(getProfileId());
		buffer.append("-->"); //$NON-NLS-1$
		switch (reason) {
			case IProfileEvent.ADDED :
				buffer.append("ADDED"); //$NON-NLS-1$
				break;
			case IProfileEvent.REMOVED :
				buffer.append("REMOVED"); //$NON-NLS-1$
				break;
			case IProfileEvent.CHANGED :
				buffer.append("CHANGED"); //$NON-NLS-1$
				break;
		}
		buffer.append("] "); //$NON-NLS-1$
		return buffer.toString();
	}
}
