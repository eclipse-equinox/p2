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

package org.eclipse.equinox.internal.p2.ui.viewers;

import java.util.EventListener;

/**
 * A listening interface used to signal when fetching begins and
 * ends.  Used by clients who wish to coordinate fetching with other
 * capabilities of the viewer.
 * 
 * @since 3.4
 *
 */
public interface IDeferredQueryTreeListener extends EventListener {

	public void fetchingDeferredChildren(Object parent, Object placeHolder);

	public void finishedFetchingDeferredChildren(Object parent, Object placeHolder);
}
