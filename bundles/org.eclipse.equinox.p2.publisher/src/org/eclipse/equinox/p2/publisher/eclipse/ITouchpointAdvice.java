/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import org.eclipse.equinox.internal.provisional.p2.metadata.TouchpointData;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

/**
 * Touchpoint advice provides information related to the touch points associated
 * with an installable unit being published.
 */
public interface ITouchpointAdvice extends IPublisherAdvice {
	/**
	 * Returns a touchpoint data that merges the given touchpoint data
	 * with any new touchpoint data contributed by this advice.
	 * 
	 * @param existingData The current set of touchpoint data.
	 * @return the merged touchpoint data
	 */
	public TouchpointData getTouchpointData(TouchpointData existingData);

}
