/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.operations;

/**
 * IStatusCodes defines codes for common status conditions when
 * performing provisioning operations.
 * 
 * This interface is not intended to be implemented
 * 
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IStatusCodes {

	//Operation status codes [10000-10999] - note these cannot conflict with the core codes
	//in ProvisionException or we'll see strange results.

	public static final int NOTHING_TO_UPDATE = 10000;

	// Status codes associated with profile change request or plans being altered from the original intent
	public static final int PROFILE_CHANGE_ALTERED = 10001;
	public static final int IU_REQUEST_ALTERED = 10002;
	public static final int ALTERED_IMPLIED_UPDATE = 10003;
	public static final int ALTERED_IGNORED_IMPLIED_DOWNGRADE = 10004;
	public static final int ALTERED_IGNORED_ALREADY_INSTALLED = 10005;
	public static final int ALTERED_PARTIAL_INSTALL = 10006;
	public static final int ALTERED_PARTIAL_UNINSTALL = 10007;
	public static final int ALTERED_SIDE_EFFECT_UPDATE = 10008;
	public static final int ALTERED_SIDE_EFFECT_REMOVE = 10009;
	public static final int ALTERED_SIDE_EFFECT_INSTALL = 10010;
	public static final int ALTERED_IGNORED_INSTALL_REQUEST = 10011;
	public static final int ALTERED_IGNORED_UNINSTALL_REQUEST = 10012;
	public static final int ALTERED_IGNORED_IMPLIED_UPDATE = 10013;

	// Status codes associated with inability to perform an operation
	public static final int UNEXPECTED_NOTHING_TO_DO = 10050;
	public static final int EXPECTED_NOTHING_TO_DO = 10051;
	public static final int OPERATION_ALREADY_IN_PROGRESS = 10052;
	public static final int MISSING_REQUIREMENTS = 10053;

	// Status codes associated with repositories
	public static final int INVALID_REPOSITORY_LOCATION = 10100;
}
