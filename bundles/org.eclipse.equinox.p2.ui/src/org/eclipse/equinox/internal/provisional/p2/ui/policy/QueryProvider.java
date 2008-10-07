/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.policy;

import org.eclipse.equinox.internal.p2.ui.model.QueriedElement;
import org.eclipse.equinox.internal.provisional.p2.ui.ElementQueryDescriptor;

/**
 * An abstract class for an object that provides element queries
 * 
 * @since 3.5
 */
public abstract class QueryProvider {

	public static final int METADATA_REPOS = 1;
	public static final int ARTIFACT_REPOS = 2;
	public static final int PROFILES = 3;
	public static final int AVAILABLE_IUS = 4;
	public static final int AVAILABLE_UPDATES = 5;
	public static final int INSTALLED_IUS = 6;

	public abstract ElementQueryDescriptor getQueryDescriptor(QueriedElement element);
}
