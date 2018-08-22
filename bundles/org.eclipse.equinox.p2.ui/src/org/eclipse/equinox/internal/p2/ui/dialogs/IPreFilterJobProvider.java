/*******************************************************************************
 *  Copyright (c) 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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
