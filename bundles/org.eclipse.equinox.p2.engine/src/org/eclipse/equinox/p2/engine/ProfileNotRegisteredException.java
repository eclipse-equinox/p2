/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.osgi.util.NLS;

/**
 * @since 2.11
 */
public class ProfileNotRegisteredException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	public ProfileNotRegisteredException(IProfile profile) {
		super(NLS.bind(Messages.profile_not_registered, profile.getProfileId()));
	}

}
