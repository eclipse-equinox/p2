/*******************************************************************************
 * Copyright (c) 2009, Cloudsmith Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.testserver.servlets;

import java.net.URI;
import java.net.URLConnection;

/**
 * Manipulates the last modified time of serviced files.
 * The manipulator will set all modified times to 0 if TYPE_ZERO is used, to 1 if TYPE_OLD is used,
 * to the current time if TYPE_NOW is used, and a value 24 hours into the future if TYPE_FUTURE is used.
 * (Future values are not allowed in HTTP - the server should guard against them and force the value
 * to be no bigger than the response date).
 *
 */
public class LastModifiedLier extends BasicResourceDelivery {

	public static final int TYPE_ZERO = 1;
	public static final int TYPE_OLD = 2;
	public static final int TYPE_NOW = 3;
	public static final int TYPE_FUTURE = 4;

	private int type;

	/**
	 * The ContentLengthLier sets the content length to a percentage of the original length.
	 * Values between 0 and 200 can be used (to lie about files being both smaller < 100, or larger > 100).
	 * 
	 * @param theAlias
	 * @param thePath
	 * @param timeType - a TYPE_XXX constant defining what time to return
	 */
	public LastModifiedLier(String theAlias, URI thePath, int timeType) {
		super(theAlias, thePath);
		if (timeType < TYPE_ZERO || timeType > TYPE_FUTURE)
			throw new IllegalArgumentException("unknown timeType, was:" + Integer.valueOf(timeType)); //$NON-NLS-1$
		type = timeType;
	}

	private static final long serialVersionUID = 1L;

	protected long getLastModified(URLConnection conn) {
		// ignore real value and lie based on constant
		return getLastModified();
	}

	private long getLastModified() {
		switch (type) {
			case TYPE_ZERO :
				return 0L;
			case TYPE_OLD :
				return 1L;
			case TYPE_NOW :
				return System.currentTimeMillis();
			case TYPE_FUTURE :
				return System.currentTimeMillis() + 24 * 60 * 60 * 1000;
		}
		// should not happen
		return 0L;
	}
}
