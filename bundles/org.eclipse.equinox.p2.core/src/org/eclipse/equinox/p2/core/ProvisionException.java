/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.Activator;

/**
 * A checked exception indicating a recoverable error occurred while provisioning.
 * The status provides a further description of the problem.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class ProvisionException extends CoreException {
	private static final long serialVersionUID = 1L;

	//General and core status codes [0-1000]
	/**
	 * Status code constant (value 1) for an uncategorized error.
	 */
	public static final int INTERNAL_ERROR = 1;

	//Repository status codes [1000-1999]
	//General repository codes [1000-1099]
	/** 
	 * Status code constant (value 1000) indicating a repository
	 * unexpectedly does not exist.
	 */
	public static final int REPOSITORY_NOT_FOUND = 1000;

	/** 
	 * Status code constant (value 1001) indicating a repository
	 * unexpectedly exists.
	 */
	public static final int REPOSITORY_EXISTS = 1001;

	/** 
	 * Status code constant (value 1002) indicating a repository
	 * could not be read
	 */
	public static final int REPOSITORY_FAILED_READ = 1002;

	/** 
	 * Status code constant (value 1003) indicating a failure occurred
	 * while writing to a repository.
	 */
	public static final int REPOSITORY_FAILED_WRITE = 1003;

	/** 
	 * Status code constant (value 1004) indicating a repository
	 * could not be written because it is a read-only repository.
	 */
	public static final int REPOSITORY_READ_ONLY = 1004;

	/** 
	 * Status code constant (value 1005) indicating an attempt was
	 * made to create or access a repository of unknown type.
	 */
	public static final int REPOSITORY_UNKNOWN_TYPE = 1005;
	/** 
	 * Status code constant (value 1006) indicating that a specified
	 * repository location is not valid.
	 */
	public static final int REPOSITORY_INVALID_LOCATION = 1006;

	/** 
	 * Status code constant (value 1007) indicating that there was
	 * an authentication error while reading a repository
	 */
	public static final int REPOSITORY_FAILED_AUTHENTICATION = 1007;

	//Metadata repository codes [1100-1199]

	//Artifact repository codes [1200-1299]

	/** 
	 * Status code constant (value 1200) indicating an artifact unexpectedly
	 * does not exist.
	 */
	public static final int ARTIFACT_NOT_FOUND = 1200;

	/** 
	 * Status code constant (value 1201) indicating an artifact unexpectedly
	 * already exists.
	 */
	public static final int ARTIFACT_EXISTS = 1201;

	/**
	 * Status code constant (value 1202) indicating an artifact's size
	 * could not be found.
	 */
	public static final int ARTIFACT_INCOMPLETE_SIZING = 1202;

	/**
	 * Status code constant (value 1203) indicating an artifact's md5
	 * does not match the expected value.
	 * @since 2.1
	 */
	public static final int ARTIFACT_MD5_NOT_MATCH = 1203;

	/**
	 * Creates a new exception with the given status object.  The message
	 * of the given status is used as the exception message.
	 *
	 * @param status the status object to be associated with this exception
	 */
	public ProvisionException(IStatus status) {
		super(status);
	}

	/**
	 * Creates a new exception with the given message and a severity of 
	 * {@link IStatus#ERROR}.
	 *
	 * @param message The human-readable problem message
	 */
	public ProvisionException(String message) {
		super(new Status(IStatus.ERROR, Activator.ID, message));
	}

	/**
	 * Creates a new exception with the given message and cause, and
	 * a severity of {@link IStatus#ERROR}.
	 *
	 * @param message The human-readable problem message
	 * @param cause The underlying cause of the exception
	 */
	public ProvisionException(String message, Throwable cause) {
		super(new Status(IStatus.ERROR, Activator.ID, message, cause));
	}

}