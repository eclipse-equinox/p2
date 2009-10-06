/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;

/**
 * 
 * IErrorReportingPage is used to report resolution
 * errors on a wizard page.
 *
 * @since 3.5
 *
 */
public interface IResolutionErrorReportingPage extends ISelectableIUsPage {
	public void updateStatus(IUElementListRoot root, PlannerResolutionOperation resolvedOperation);
}
