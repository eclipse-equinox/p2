/*******************************************************************************
 * Copyright (c) 2012, 2016 Wind River and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.testserver.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.p2.testserver.HttpConstants;

public class Stats extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3209768955270841029L;
	private static final String UnkownPackage = "unkown"; //$NON-NLS-1$
	private static final String PACKAGE = "package"; //$NON-NLS-1$
	private Map downloadStats = new HashMap();

	public Stats() {
		downloadStats.put(UnkownPackage, Integer.valueOf(0));
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		responseHeaderAndStatus(response);

		final String result = getStatsResult();
		response.setContentLength(result.length());
		response.setContentType("text/plain"); //$NON-NLS-1$

		BufferedReader reader = new BufferedReader(new StringReader(result));
		PrintWriter writer = response.getWriter();
		try {
			writer.flush(); /* write the headers and unbuffer the output */
			char buffer[] = new char[4096];
			int read;
			while ((read = reader.read(buffer, 0, buffer.length)) != -1) {
				writer.write(buffer, 0, read);
			}
		} finally {
			reader.close();
			writer.close();
		}
	}

	private void responseHeaderAndStatus(HttpServletResponse response) {
		// set when the resource was modified
		DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US); //$NON-NLS-1$
		// must always be GMT
		df.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
		response.setHeader(HttpConstants.LAST_MODIFIED, df.format(new Date()));
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private String getStatsResult() {
		StringBuffer sb = new StringBuffer();
		sb.append("It's a page to count the downloading times when heading this page with query 'package=<package name>'.\n"); //$NON-NLS-1$
		for (Iterator iter = downloadStats.keySet().iterator(); iter.hasNext();) {
			String packageName = (String) iter.next();
			sb.append(packageName).append(" download number: ").append(downloadStats.get(packageName)).append("\n"); //$NON-NLS-1$//$NON-NLS-2$
		}
		return sb.toString();
	}

	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String queryString = request.getQueryString();

		responseHeaderAndStatus(response);

		boolean found = false;
		if (queryString != null) {
			String[] parameters = queryString.split("&"); //$NON-NLS-1$
			for (int i = 0; i < parameters.length; i++) {
				String[] paraPair = parameters[i].split("=", 2); //$NON-NLS-1$
				if (paraPair.length == 2 && PACKAGE.equals(paraPair[0])) {
					found = true;
					Integer count = (Integer) downloadStats.get(paraPair[1]);
					if (count == null) {
						count = Integer.valueOf(1);
					} else
						count = Integer.valueOf(1 + count.intValue());
					downloadStats.put(paraPair[1], count);
					break;
				}
			}
		}
		if (!found) {
			Integer count = (Integer) downloadStats.get(UnkownPackage);
			downloadStats.put(UnkownPackage, Integer.valueOf(1 + count.intValue()));
		}
	}
}
