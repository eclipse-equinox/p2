/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.Dictionary;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;

/**
 * Helper class for creating a Projector. Introduced in 3.4.x maintenance stream to help with bug 303936.
 * Default behaviour is to create the same projector as is used in the 3.4.x stream. If the user specifies
 * the eclipse.p2.newProjector=true System property then we create a different projector, one which
 * is based on changes in the 3.5.x stream.
 * 
 * @since 1.0.6
 */
public class ProjectorFactory {

	/**
	 * Create and return a new projector.
	 */
	public static IProjector create(IQueryable q, Dictionary context) {
		String value = DirectorActivator.context.getProperty("eclipse.p2.newProjector"); //$NON-NLS-1$
		return Boolean.valueOf(value).booleanValue() ? new Projector2(q, context) : (IProjector) new Projector(q, context);
	}
}
