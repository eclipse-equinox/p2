/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;

/**
 * 
 * IErrorReportingPage is used to report resolution
 * errors on a wizard page.
 *
 * @since 3.5
 *
 */
public interface IResolutionErrorReportingPage extends ISelectableIUsPage {
	public void updateStatus(IUElementListRoot root, ProfileChangeOperation operation);
}
