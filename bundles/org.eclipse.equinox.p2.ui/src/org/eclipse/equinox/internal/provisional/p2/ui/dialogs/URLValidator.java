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

package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * 
 * URLValidator can be used to validate a repository URL.  Validation may
 * involve rules known by the validator itself or contact with a repository
 * manager.
 * 
 * @since 3.4
 *
 */
public abstract class URLValidator {
	public static final String FILE_PROTOCOL = "file"; //$NON-NLS-1$
	public static final String FILE_PROTOCOL_PREFIX = "file:"; //$NON-NLS-1$
	public static final String JAR_PATH_PREFIX = "jar:";//$NON-NLS-1$
	public static final String JAR_PATH_SUFFIX = "!/"; //$NON-NLS-1$

	public static final int LOCAL_VALIDATION_ERROR = 3000;
	public static final int REPO_AUTO_GENERATED = 3001;
	public static final int ALTERNATE_ACTION_TAKEN = 3002;

	protected abstract IStatus validateRepositoryURL(URL url, boolean contactRepositories, IStatus originalStatus, IProgressMonitor monitor);
}
