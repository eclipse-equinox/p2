package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.equinox.p2.internal.repository.tools.SlicingOptions;

public class SlicingOption extends Task {

	SlicingOptions options = null;

	public SlicingOption() {
		options = new SlicingOptions();
		options.forceFilterTo(true);
		options.considerStrictDependencyOnly(false);
		options.everythingGreedy(true);
		options.includeOptionalDependencies(true);
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
			throw new BuildException("Invalid platform filter format: " + platformFilter + ".");
		Dictionary filter = new Properties();
		filter.put("osgi.os", tok.nextToken().trim()); //$NON-NLS-1$
		filter.put("osgi.ws", tok.nextToken().trim()); //$NON-NLS-1$
		filter.put("osgi.arch", tok.nextToken().trim()); //$NON-NLS-1$
	}

	public void setIncludeNonGreedy(boolean greed) {
		options.everythingGreedy(greed);
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
