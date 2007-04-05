package org.eclipse.equinox.internal.simpleconfigurator.console;

import java.net.MalformedURLException;
import java.net.URL;

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
			return new URL(urlString);
		} catch (MalformedURLException e) {
			interpreter.println(e.getMessage());
			return null;
		}
	}

	/**
	 * Apply the current configuration
	 * @param configuration URL (optional)
	 */
	public void _confapply(CommandInterpreter interpreter) {	
		String parameter = interpreter.nextArgument();
		URL configURL = null;
		if (parameter != null)
			configURL  = toURL(interpreter, parameter);
		
		new ApplyCommand(interpreter, context, configURL).run();
	}
	
	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append(NEW_LINE);
		help.append("---"); //$NON-NLS-1$
		help.append("Configurator Commands");
		help.append("---"); //$NON-NLS-1$
		help.append(NEW_LINE);
		help.append("\tconfapply [<config URL>] - Applies a configuration");		
		return help.toString();
	}
}
