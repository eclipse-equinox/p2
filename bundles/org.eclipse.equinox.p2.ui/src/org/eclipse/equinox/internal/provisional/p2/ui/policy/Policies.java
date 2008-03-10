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
package org.eclipse.equinox.internal.provisional.p2.ui.policy;


/**
 * Abstract class for a set of policies provided by applications that
 * are used for decision making during provisioning operations.
 * 
 * @since 3.4
 */
public abstract class Policies {

	public abstract IQueryProvider getQueryProvider();

	public abstract LicenseManager getLicenseManager();

	public abstract IPlanValidator getPlanValidator();
}
