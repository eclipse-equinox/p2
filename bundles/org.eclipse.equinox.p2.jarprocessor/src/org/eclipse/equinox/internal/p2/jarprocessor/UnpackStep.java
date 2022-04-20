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
import java.util.List;
import java.util.Properties;

/**
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 * @deprecated See <a href=
 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
 *             and <a href=
 *             "https://github.com/eclipse-equinox/p2/issues/40">issue</a> for
 *             details.
 */
@Deprecated(forRemoval = true, since = "1.2.0")
public class UnpackStep extends CommandStep {
	public static final String UNPACKER_PROPERTY = "org.eclipse.update.jarprocessor.Unpacker"; //$NON-NLS-1$

	public static boolean canUnpack() {
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

	@Override
	public String getStepName() {
		return "Unpack (NO-OP see https://github.com/eclipse-equinox/p2/issues/40)"; //$NON-NLS-1$
	}
}
