/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.console;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.equinox.internal.simpleconfigurator.utils.Utils;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleContext;

public class ConfiguratorCommandProvider implements CommandProvider {
	public static final String NEW_LINE = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

	private BundleContext context;

	public ConfiguratorCommandProvider(BundleContext context) {
		this.context = context;
	}

	/**
	 * Returns the given string as an URL, or <code>null</code> if
	 * the string could not be interpreted as an URL.
	 */
	private URL toURL(CommandInterpreter interpreter, String urlString) {
		try {
			return Utils.buildURL(urlString);
		} catch (MalformedURLException e) {
			interpreter.println(e.getMessage());
			return null;
		}
	}

	/**
	 * Apply the current configuration
	 * @param interpreter 
	 */
	public void _confapply(CommandInterpreter interpreter) {
		String parameter = interpreter.nextArgument();
		URL configURL = null;
		if (parameter != null)
			configURL = toURL(interpreter, parameter);

		new ApplyCommand(interpreter, context, configURL).run();
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append("---"); //$NON-NLS-1$
		help.append("Configurator Commands"); //$NON-NLS-1$
		help.append("---"); //$NON-NLS-1$
		help.append(NEW_LINE);
		help.append("\tconfapply [<config URL>] - Applies a configuration"); //$NON-NLS-1$
		help.append(NEW_LINE);
		return help.toString();
	}
}
