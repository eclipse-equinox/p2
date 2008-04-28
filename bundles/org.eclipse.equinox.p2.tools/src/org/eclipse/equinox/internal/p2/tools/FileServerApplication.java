/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.tools;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.osgi.service.http.*;

public class FileServerApplication implements IApplication {
	private Map resources = new HashMap(10);

	private static class FileSystemContext implements HttpContext {
		private String base;

		public FileSystemContext(String base) {
			this.base = base;
		}

		public String getMimeType(String name) {
			return null;
		}

		public URL getResource(String name) {
			try {
				return new URL(base + name);
			} catch (MalformedURLException e) {
				return null;
			}
		}

		public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
			return true;
		}
	}

	public Object start(IApplicationContext context) throws Exception {
		Map args = context.getArguments();
		initializeFromArguments((String[]) args.get("application.args")); //$NON-NLS-1$
		registerResources(resources);
		return null;
	}

	private void registerResources(Map list) {
		HttpService http = (HttpService) ServiceHelper.getService(Activator.getContext(), HttpService.class.getName());
		for (Iterator i = resources.keySet().iterator(); i.hasNext();) {
			String key = (String) i.next();
			String value = (String) resources.get(key);
			try {
				http.registerResources(key, "/", new FileSystemContext(value)); //$NON-NLS-1$
			} catch (NamespaceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void stop() {
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)
			//			if (args[i].equals("-raw"))
			//				raw = true;

			// check for args with parameters. If we are at the last argument or 
			// if the next one has a '-' as the first character, then we can't have 
			// an arg with a param so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase("-resource")) //$NON-NLS-1$
				resources.put(arg, args[++i]);
		}
	}
}
