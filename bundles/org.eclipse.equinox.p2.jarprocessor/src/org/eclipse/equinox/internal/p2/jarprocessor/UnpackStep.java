/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.jarprocessor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class UnpackStep extends CommandStep {
	public static final String UNPACKER_PROPERTY = "org.eclipse.update.jarprocessor.Unpacker"; //$NON-NLS-1$
	private static Boolean canUnpack = null;
	private static String unpackCommand = null;

	public static boolean canUnpack() {
		if (canUnpack != null)
			return canUnpack.booleanValue();

		String[] locations = Utils.getPack200Commands("unpack200"); //$NON-NLS-1$
		if (locations == null) {
			canUnpack = Boolean.FALSE;
			unpackCommand = null;
			return false;
		}

		int result;
		for (int i = 0; i < locations.length; i++) {
			if (locations[i] == null)
				continue;
			result = execute(new String[] {locations[i], "-V"}); //$NON-NLS-1$
			if (result == 0) {
				unpackCommand = locations[i];
				canUnpack = Boolean.TRUE;
				return true;
			}
		}

		canUnpack = Boolean.FALSE;
		return false;
	}

	public UnpackStep(Properties options) {
		super(options, null, null, false);
	}

	public UnpackStep(Properties options, boolean verbose) {
		super(options, null, null, verbose);
	}

	@Override
	public String recursionEffect(String entryName) {
		if (canUnpack() && entryName.endsWith(Utils.PACKED_SUFFIX)) {
			return entryName.substring(0, entryName.length() - Utils.PACKED_SUFFIX.length());
		}
		return null;
	}

	@Override
	public File preProcess(File input, File workingDirectory, List<Properties> containers) {
		if (canUnpack() && unpackCommand != null) {
			String name = input.getName();
			if (name.endsWith(Utils.PACKED_SUFFIX)) {
				name = name.substring(0, name.length() - Utils.PACKED_SUFFIX.length());

				File unpacked = new File(workingDirectory, name);
				File parent = unpacked.getParentFile();
				if (!parent.exists())
					parent.mkdirs();
				try {
					String options = getOptions().getProperty(input.getName() + ".unpack.args"); //$NON-NLS-1$
					String[] cmd = null;
					if (options != null) {
						cmd = new String[] {unpackCommand, options, input.getCanonicalPath(), unpacked.getCanonicalPath()};
					} else {
						cmd = new String[] {unpackCommand, input.getCanonicalPath(), unpacked.getCanonicalPath()};
					}
					int result = execute(cmd, verbose);
					if (result != 0 && verbose)
						System.out.println("Error: " + result + " was returned from command: " + Utils.concat(cmd)); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (IOException e) {
					if (verbose)
						e.printStackTrace();
					return null;
				}
				return unpacked;
			}
		}
		return null;
	}

	@Override
	public File postProcess(File input, File workingDirectory, List<Properties> containers) {
		return null;
	}

	@Override
	public String getStepName() {
		return "Unpack"; //$NON-NLS-1$
	}
}
