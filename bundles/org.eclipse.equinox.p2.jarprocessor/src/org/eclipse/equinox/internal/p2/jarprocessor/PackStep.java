/*******************************************************************************
 *  Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.jarprocessor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @deprecated See <a href=
 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
 *             and <a href=
 *             "https://github.com/eclipse-equinox/p2/issues/40">issue</a> for
 *             details.
 */
@Deprecated(forRemoval = true, since = "1.2.0")
public class PackStep extends CommandStep {

	protected static String packCommand = null;

	public static boolean canPack() {
		// Ignore everything pack200 https://github.com/eclipse-equinox/p2/issues/40
		return false;
	}

	public PackStep(Properties options) {
		super(options, null, null, false);
	}

	public PackStep(Properties options, boolean verbose) {
		super(options, null, null, verbose);
	}

	@Override
	public String recursionEffect(String entryName) {
		return null;
	}

	@Override
	public File preProcess(File input, File workingDirectory, List<Properties> containers) {
		return null;
	}

	@Override
	public File postProcess(File input, File workingDirectory, List<Properties> containers) {
		return null;
	}

	protected boolean shouldPack(File input, List<Properties> containers, Properties inf) {
		return false;
	}

	protected String[] getCommand(File input, File outputFile, Properties inf, List<Properties> containers)
			throws IOException {
		String[] cmd = null;
		String arguments = getArguments(input, inf, containers);
		if (arguments != null && arguments.length() > 0) {
			String[] args = Utils.toStringArray(arguments, ","); //$NON-NLS-1$
			cmd = new String[3 + args.length];
			cmd[0] = packCommand;
			System.arraycopy(args, 0, cmd, 1, args.length);
			cmd[cmd.length - 2] = outputFile.getCanonicalPath();
			cmd[cmd.length - 1] = input.getCanonicalPath();
		} else {
			cmd = new String[] { packCommand, outputFile.getCanonicalPath(), input.getCanonicalPath() };
		}
		return cmd;
	}

	protected String getArguments(File input, Properties inf, List<Properties> containers) {
		// 1: Explicitly marked in our .inf file
		if (inf != null && inf.containsKey(Utils.PACK_ARGS)) {
			return inf.getProperty(Utils.PACK_ARGS);
		}

		// 2: Defaults set in one of our containing jars
		for (Properties container : containers) {
			if (container.containsKey(Utils.DEFAULT_PACK_ARGS)) {
				return container.getProperty(Utils.DEFAULT_PACK_ARGS);
			}
		}

		// 3: Set by name in outside pack.properties file
		Properties options = getOptions();
		String argsKey = input.getName() + Utils.PACK_ARGS_SUFFIX;
		if (options.containsKey(argsKey)) {
			return options.getProperty(argsKey);
		}

		// 4: Set by default in outside pack.properties file
		if (options.containsKey(Utils.DEFAULT_PACK_ARGS)) {
			return options.getProperty(Utils.DEFAULT_PACK_ARGS);
		}

		return ""; //$NON-NLS-1$
	}

	@Override
	public String getStepName() {
		return "Pack (NO-OP see https://github.com/eclipse-equinox/p2/issues/40)"; //$NON-NLS-1$
	}

	@Override
	public boolean adjustInf(File input, Properties inf, List<Properties> containers) {
		if (input == null || inf == null)
			return false;

		// don't be verbose to check if we should mark the inf
		boolean v = verbose;
		verbose = false;
		if (!shouldPack(input, containers, inf)) {
			verbose = v;
			return false;
		}
		verbose = v;

		// mark as conditioned if not previously marked. A signed jar is assumed to be
		// previously conditioned.
		if (inf.getProperty(Utils.MARK_PROPERTY) != null)
			return false;

		inf.put(Utils.MARK_PROPERTY, "true"); //$NON-NLS-1$
		// record arguments used
		String arguments = inf.getProperty(Utils.PACK_ARGS);
		if (arguments == null) {
			arguments = getArguments(input, inf, containers);
			if (arguments != null && arguments.length() > 0)
				inf.put(Utils.PACK_ARGS, arguments);
		}
		return true;
	}
}
