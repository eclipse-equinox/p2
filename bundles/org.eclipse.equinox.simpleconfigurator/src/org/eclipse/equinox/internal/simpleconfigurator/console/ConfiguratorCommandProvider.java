/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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
	public static final String NEW_LINE = "\r\n"; //$NON-NLS-1$

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
	 */
	public void _confapply(CommandInterpreter interpreter) {
		String parameter = interpreter.nextArgument();
		URL configURL = null;
		if (parameter != null)
			configURL = toURL(interpreter, parameter);

		new ApplyCommand(interpreter, context, configURL).run();
	}

	/**
	 * Handles the help command
	 *
	 * @return description for a particular command or false if there is no command with the specified name
	 */
	public Object _help(CommandInterpreter intp) {
		String commandName = intp.nextArgument();
		if (commandName == null) {
			return Boolean.FALSE;
		}
		String help = getHelp(commandName);

		if (help.length() > 0) {
			return help;
		}
		return Boolean.FALSE;
	}

	@Override
	public String getHelp() {
		return getHelp(null);
	}

	private String getHelp(String commandName) {
		StringBuilder help = new StringBuilder();

		if (commandName == null) {
			help.append("---"); //$NON-NLS-1$
			help.append("Configurator Commands"); //$NON-NLS-1$
			help.append("---"); //$NON-NLS-1$
			help.append(NEW_LINE);
		}

		if (commandName == null || "confapply".equals(commandName)) {
			help.append("\tconfapply [<config URL>] - Applies a configuration"); //$NON-NLS-1$
			help.append(NEW_LINE);
		}

		return help.toString();
	}
}
