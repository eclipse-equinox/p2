/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.Map;
import java.util.StringTokenizer;
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
		options.followOnlyFilteredRequirements(false);
		options.installTimeLikeResolution(false);
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
		Map<String, String> filter = options.getFilter();
		filter.put("osgi.os", tok.nextToken().trim()); //$NON-NLS-1$
		filter.put("osgi.ws", tok.nextToken().trim()); //$NON-NLS-1$
		filter.put("osgi.arch", tok.nextToken().trim()); //$NON-NLS-1$
		options.setFilter(filter);
	}

	public void setIncludeNonGreedy(boolean greed) {
		options.everythingGreedy(greed);
	}

	public void setIncludeFeatures(boolean includeFeatures) {
		Map<String, String> filter = options.getFilter();
		filter.put("org.eclipse.update.install.features", String.valueOf(includeFeatures)); //$NON-NLS-1$
		options.setFilter(filter);
	}

	public void setFilter(String filterString) {
		if (filterString == null || filterString.trim().equals("")) //$NON-NLS-1$
			return;
		Map<String, String> filter = options.getFilter();
		StringTokenizer tok = new StringTokenizer(filterString, ","); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String rule = tok.nextToken().trim();
			int eqIndex = rule.indexOf('=');
			if (eqIndex == -1)
				throw new BuildException(NLS.bind(Messages.SlicingOption_invalidFilterFormat, rule));
			filter.put(rule.substring(0, eqIndex), rule.substring(eqIndex + 1));
		}
		options.setFilter(filter);
	}

	/** 
	 * Set this property to true if only strict dependencies must be followed. A strict dependency is defined by a version range only including one version (e.g. [1.0.0.v2009, 1.0.0.v2009])
	 * The default value is false.
	 */
	public void setFollowStrict(boolean strict) {
		options.considerStrictDependencyOnly(strict);
	}

	public void setFollowOnlyFilteredRequirements(boolean onlyFiltered) {
		options.followOnlyFilteredRequirements(onlyFiltered);
	}

	public void setLatestVersionOnly(boolean latest) {
		options.latestVersionOnly(latest);
	}

	public void setResolve(boolean resolve) {
		options.installTimeLikeResolution(resolve);
	}

	public SlicingOptions getOptions() {
		return options;
	}

}
