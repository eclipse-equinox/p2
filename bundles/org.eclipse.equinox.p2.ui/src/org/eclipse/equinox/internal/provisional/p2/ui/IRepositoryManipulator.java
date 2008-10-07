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

package org.eclipse.equinox.internal.provisional.p2.ui;

import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.URLValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator;
import org.eclipse.swt.widgets.Shell;

/**
 * @since 3.4
 * @deprecated temp hack class to transition pde ui
 *
 */
public abstract class IRepositoryManipulator extends RepositoryManipulator {
	public abstract URLValidator getURLValidator(Shell shell);

	public org.eclipse.equinox.internal.provisional.p2.ui.policy.URLValidator getRepositoryURLValidator(Shell shell) {
		return new org.eclipse.equinox.internal.provisional.p2.ui.policy.URLValidator() {

			public IStatus validateRepositoryURL(URL url, boolean contactRepositories, IProgressMonitor monitor) {
				return new org.eclipse.equinox.internal.p2.ui.DefaultMetadataURLValidator().validateRepositoryURL(url, contactRepositories, monitor);
			}

		};
	}
}
