/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.internal.repository.tools.SlicingOptions;
import org.eclipse.osgi.util.NLS;

public class SlicingOption extends Task {

	SlicingOptions options = null;

	public SlicingOption() {
		options = new SlicingOptions();
		options.forceFilterTo(true);
		options.considerStrictDependencyOnly(false);
		options.everythingGreedy(true);
		options.includeOptionalDependencies(true);
		setIncludeFeatures(true);
	}

	/**
	 * Setting this to true will cause the optional dependencies to be considered.
	 */
	public void setIncludeOptional(boolean optional) {
		options.includeOptionalDependencies(optional);
	}

	/**
	 * 
	 */
	public void setPlatformFilter(String platformFilter) {
		if (platformFilter == null || platformFilter.trim().equals("")) //$NON-NLS-1$
			return;
		if (platformFilter.equalsIgnoreCase("true")) { //$NON-NLS-1$
			options.forceFilterTo(true);
			return;
		}
		if (platformFilter.equalsIgnoreCase("false")) { //$NON-NLS-1$
			options.forceFilterTo(false);
			return;
		}
		StringTokenizer tok = new StringTokenizer(platformFilter, ","); //$NON-NLS-1$
		if (tok.countTokens() != 3)
			throw new BuildException(NLS.bind(Messages.SlicingOption_invalid_platform, platformFilter));
		Dictionary filter = options.getFilter();
		if (filter == null)
			filter = new Properties();
		filter.put("osgi.os", tok.nextToken().trim()); //$NON-NLS-1$
		filter.put("osgi.ws", tok.nextToken().trim()); //$NON-NLS-1$
		filter.put("osgi.arch", tok.nextToken().trim()); //$NON-NLS-1$
		options.setFilter(filter);
	}

	public void setIncludeNonGreedy(boolean greed) {
		options.everythingGreedy(greed);
	}

	public void setIncludeFeatures(boolean includeFeatures) {
		Dictionary filter = options.getFilter();
		if (filter == null)
			filter = new Properties();
		filter.put("org.eclipse.update.install.features", String.valueOf(includeFeatures)); //$NON-NLS-1$
		options.setFilter(filter);
	}

	/** 
	 * Set this property to true if only strict dependencies must be followed. A strict dependency is defined by a version range only including one version (e.g. [1.0.0.v2009, 1.0.0.v2009])
	 * The default value is false.
	 */
	public void setFollowStrict(boolean strict) {
		options.considerStrictDependencyOnly(strict);
	}

	public SlicingOptions getOptions() {
		return options;
	}
}
