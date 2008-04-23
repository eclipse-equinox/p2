/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import org.eclipse.equinox.internal.p2.metadata.generator.EclipseGeneratorApplication;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.Generator.GeneratorResult;

/**
 * A class to enable carrying GeneratorResults across multiple invocations of the Generator.
 * Done here in the bundle instead of in GeneratorTask because of the way org.eclipse.ant.core.AntRunner uses class loaders.
 * @since 1.0
 */

public class IncrementalGenerator {
	private static String MODE_INCREMENTAL = "incremental"; //$NON-NLS-1$
	private String mode = null;
	static private GeneratorResult result = null;

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void run(EclipseGeneratorApplication generator, EclipseInstallGeneratorInfoProvider provider) throws Exception {
		if (MODE_INCREMENTAL.equals(mode)) {
			if (result == null)
				result = new GeneratorResult();
			generator.setIncrementalResult(result);
			generator.setGeneratorRootIU(false);
		} else if ("final".equals(mode) && result != null) { //$NON-NLS-1$
			generator.setIncrementalResult(result);
			generator.setGeneratorRootIU(true);
		}

		generator.run(provider);

		if (!MODE_INCREMENTAL.equals(mode)) {
			result = null;
		}
	}

}
