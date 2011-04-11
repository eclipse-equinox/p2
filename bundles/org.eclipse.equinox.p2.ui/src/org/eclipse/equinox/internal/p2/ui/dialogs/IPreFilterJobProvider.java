/*******************************************************************************
 *  Copyright (c) 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.runtime.jobs.Job;

/**
 * IPreFilterJobProvider provides an optional job that must be run before
 * filtering can be allowed to occur in a filtered tree.  The client is assumed
 * to have set the expected job priority.
 * 
 */
public interface IPreFilterJobProvider {
	public Job getPreFilterJob();
}
