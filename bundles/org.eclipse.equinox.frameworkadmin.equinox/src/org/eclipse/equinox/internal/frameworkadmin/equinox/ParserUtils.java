package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.osgi.service.log.LogService;

public class ParserUtils {
	public static File getOSGiInstallArea(LauncherData launcherData) {
		if (launcherData == null)
			return null;

		//TODO This is not enough because if you only have -startup then osgi.install.area from the config.ini is used
		File result = getOSGiInstallArea(launcherData.getProgramArgs());
		if (result != null)
			return result;

		if (launcherData.getFwJar() != null)
			return fromOSGiJarToOSGiInstallArea(launcherData.getFwJar().getAbsolutePath());
		if (launcherData.getLauncher() != null)
			return launcherData.getLauncher().getParentFile();
		return null;
	}

	public static URI getFrameworkJar(String[] lines, URI launcherFolder) {
		String fwk = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_FW, lines);
		if (fwk == null) {
			//Search the file system using the default location
			URI location = FileUtils.getEclipsePluginFullLocation(EquinoxConstants.FW_SYMBOLIC_NAME, new File(URIUtil.toFile(launcherFolder), EquinoxConstants.PLUGINS_DIR));
			if (location != null)
				return location;
			return null;
		}
		try {
			return URIUtil.makeAbsolute(URIUtil.fromString(fwk), launcherFolder);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make absolute of:" + fwk);
			return null;
		}
	}

	public static File getOSGiInstallArea(String[] args) {
		if (args == null)
			return null;
		String install = getValueForArgument(EquinoxConstants.OPTION_INSTALL, args);
		if (install != null)
			return new File(install);
		String startup = getValueForArgument(EquinoxConstants.OPTION_STARTUP, args);
		if (startup != null)
			return fromOSGiJarToOSGiInstallArea(startup);
		return null;
	}

	private static File fromOSGiJarToOSGiInstallArea(String path) {
		IPath parentFolder = new Path(path).removeLastSegments(1);
		if (parentFolder.lastSegment().equalsIgnoreCase("plugins")) //$NON-NLS-1$
			return parentFolder.removeLastSegments(1).toFile();
		return parentFolder.toFile();
	}

	public static boolean isArgumentSet(String arg, String[] args) {
		if (arg == null || args == null || args.length == 0)
			return false;
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null)
				continue;
			if (args[i].equalsIgnoreCase(arg)) {
				return true;
			}
		}
		return false;
	}

	public static String getValueForArgument(String arg, String[] args) {
		if (arg == null || args == null || args.length == 0)
			return null;
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null)
				continue;
			if (args[i].equalsIgnoreCase(arg)) {
				if (i + 1 < args.length && args[i + 1] != null && args[i + 1].charAt(1) != '-')
					return args[i + 1];
			}
		}
		return null;
	}

	public static String[] getMultiValuedArgument(String arg, String[] args) {
		if (arg == null || args == null || args.length == 0)
			return null;
		ArrayList values = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null)
				continue;
			if (arg.equalsIgnoreCase(args[i])) {
				values = new ArrayList();
				continue;
			}
			if (values != null && args[i].charAt(1) == '-') {
				break;
			}
			if (values != null)
				values.add(args[i].trim());
		}
		if (values != null)
			return (String[]) values.toArray(new String[values.size()]);
		return null;
	}

	public static boolean setValueForArgument(String arg, String value, String[] args) {
		if (arg == null || args == null || args.length == 0)
			return false;
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null)
				continue;
			String currentArg = args[i].trim();
			if (currentArg.equalsIgnoreCase(arg)) {
				if (i + 1 < args.length && args[i + 1] != null && args[i + 1].charAt(1) != '-') {
					args[i + 1] = value;
					return true;
				}
			}
		}
		return false;
	}
}
