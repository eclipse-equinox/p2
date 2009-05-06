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
import javax.servlet.http.HttpServletResponse;

/**
 * Manipulates the last modified time of serviced files.
 * The manipulator will set all modified times to 0 if TYPE_ZERO is used, to 1 if TYPE_OLD is used,
 * to the current time if TYPE_NOW is used, and a value 24 hours into the future if TYPE_FUTURE is used.
 * (Future values are not allowed in HTTP - the server should guard against them and force the value
 * to be no bigger than the response date).
 * The TYPE_BAD will produce a HTTP header with wrong format for the date value (fails date parsing).
 *
 */
public class LastModifiedLier extends BasicResourceDelivery {

	public static final int TYPE_ZERO = 1;
	public static final int TYPE_OLD = 2;
	public static final int TYPE_NOW = 3;
	public static final int TYPE_FUTURE = 4;
	public static final int TYPE_BAD = 5;
	private static final int TYPE_LAST = TYPE_BAD;

	private int type;

	/**
	 * The LastModifiedLier returns a last modified time according to the parameter timeType.
	 * It can be TYPE_ZERO, TYPE_OLD, TYPE_NOW, TYPE_FUTURE, or TYPE_BAD.
	 * 
	 * @param theAlias
	 * @param thePath
	 * @param timeType - a TYPE_XXX constant defining what time to return
	 */
	public LastModifiedLier(String theAlias, URI thePath, int timeType) {
		super(theAlias, thePath);
		if (timeType < TYPE_ZERO || timeType > TYPE_LAST)
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
				return 1000L;
			case TYPE_NOW :
				return System.currentTimeMillis();
			case TYPE_FUTURE :
				return System.currentTimeMillis() + 24 * 60 * 60 * 1000;
		}
		// should not happen
		return 0L;
	}

	public void addDateHeader(HttpServletResponse response, String name, long timestamp) {
		if (type != TYPE_BAD)
			super.addDateHeader(response, name, timestamp);
		else
			response.setHeader(name, "intentionally-bad-date"); //$NON-NLS-1$
	}

}
