package org.eclipse.equinox.internal.p2.publisher.actions;

import org.eclipse.equinox.internal.p2.publisher.AbstractPublishingAction;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAdvice;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class AbstractAdvice implements IPublishingAdvice {

	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		return matchConfig(configSpec, includeDefault) && matchId(id) && matchVersion(version);
	}

	protected boolean matchVersion(Version version) {
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
		String[] full = AbstractPublishingAction.parseConfigSpec(configSpec);
		String[] partial = AbstractPublishingAction.parseConfigSpec(adviceConfigSpec);
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
