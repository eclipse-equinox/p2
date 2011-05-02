/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.query;

import org.eclipse.equinox.internal.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElementWrapper;
import org.eclipse.equinox.p2.engine.IProfile;

/**
 * Collector that accepts the matched Profiles and
 * wraps them in a ProfileElement.
 * 
 * @since 3.4
 */
public class ProfileElementWrapper extends QueriedElementWrapper {

	public ProfileElementWrapper(IProfile profile, Object parent) {
		super(profile, parent);
	}

	protected boolean shouldWrap(Object match) {
		if ((match instanceof IProfile))
			return true;
		return false;
	}

	/**
	 * Transforms the item to a UI element
	 */
	protected Object wrap(Object item) {
		return super.wrap(new ProfileElement(parent, ((IProfile) item).getProfileId()));
	}

}
