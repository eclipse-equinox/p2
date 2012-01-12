/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.core.UIServices.AuthenticationInfo;

/**
 * This class is only intended for the R37x branch, where the constant 
 * <code>AUTHENTICATION_PROMPT_CANCELED</code> cannot be added to the API class 
 * {@link UIServices}. Adding new API would require a minor version increment, 
 * which is not allowed in maintenance branches.
 */
public final class UIServicesHelper_R37x {
	/**
	 * This constant may be returned by the <code>getUsernamePassword</code> methods if the user 
	 * explicitly canceled the authentication prompt.
	 * 
	 * @see UIServices#getUsernamePassword(String)
	 * @see UIServices#getUsernamePassword(String, AuthenticationInfo)
	 */
	public static final AuthenticationInfo AUTHENTICATION_PROMPT_CANCELED = new AuthenticationInfo("", "", false); //$NON-NLS-1$//$NON-NLS-2$

}
