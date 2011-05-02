/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;

public class AbstractAdvice implements IPublisherAdvice {

	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		return matchConfig(configSpec, includeDefault) && matchId(id) && matchVersion(version);
	}

	protected boolean matchVersion(Version version) {
		if (version == null)
			return true;
		Version adviceVersion = getVersion();
		if (adviceVersion != null)
			return version.equals(adviceVersion);
		VersionRange range = getVersionRange();
		if (range != null)
			return range.isIncluded(version);
		return true;
	}

	protected Version getVersion() {
		return null;
	}

	protected VersionRange getVersionRange() {
		return null;
	}

	protected boolean matchId(String id) {
		if (id == null)
			return true;
		String adviceId = getId();
		return adviceId == null ? true : adviceId.equals(id);
	}

	protected String getId() {
		return null;
	}

	protected boolean matchConfig(String configSpec, boolean includeDefault) {
		String adviceConfigSpec = getConfigSpec();
		if (adviceConfigSpec == null)
			return includeDefault;
		String[] full = AbstractPublisherAction.parseConfigSpec(configSpec);
		String[] partial = AbstractPublisherAction.parseConfigSpec(adviceConfigSpec);
		for (int i = 0; i < partial.length; i++) {
			String string = partial[i];
			if (string != null && !string.equals(full[i]))
				return false;
		}
		return true;
	}

	protected String getConfigSpec() {
		return null;
	}

}
