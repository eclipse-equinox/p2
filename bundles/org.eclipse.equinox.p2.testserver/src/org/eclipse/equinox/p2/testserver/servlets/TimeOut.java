/*******************************************************************************
 * Copyright (c) 2009, 2018 Cloudsmith Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.testserver.servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet is a "black hole" - it just sleeps for an hour on any request.
 *
 */
public class TimeOut extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static long MINUTES_MS = 1000 * 60; // minutes in ms

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		doDelay();
	}

	private void doDelay() {
		// Do nothing...
		try {
			Thread.sleep(10 * MINUTES_MS); // sleep 10 minutes
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) {
		doDelay();
	}

}
