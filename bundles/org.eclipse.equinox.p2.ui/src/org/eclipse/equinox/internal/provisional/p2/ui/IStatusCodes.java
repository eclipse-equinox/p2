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
package org.eclipse.equinox.internal.provisional.p2.ui;

/**
 * IStatusCodes defines codes for common status conditions in the
 * p2 UI.
 * 
 * This interface is not intended to be implemented
 * 
 * @since 3.4
 */
public interface IStatusCodes {

	//UI status codes [10000-10999] - note these cannot conflict with the core codes
	//in ProvisionException or we'll see strange results.

	public static final int NOTHING_TO_UPDATE = 10000;
	public static final int PROFILE_CHANGE_ALTERED = 10001;
	public static final int IMPLIED_UPDATE = 10002;
	public static final int IGNORED_IMPLIED_DOWNGRADE = 10003;
	public static final int IGNORED_ALREADY_INSTALLED = 10004;
	public static final int UNEXPECTED_NOTHING_TO_DO = 10005;
}
