package org.eclipse.equinox.p2.ql;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public interface ITranslationSupport {
	String getIUProperty(IInstallableUnit iu, String key);
}
