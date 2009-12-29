package org.eclipse.equinox.internal.p2.ql;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ql.ITranslationSupport;

public class TranslationSupport extends org.eclipse.equinox.internal.p2.metadata.TranslationSupport implements ITranslationSupport {

	public String getIUProperty(IInstallableUnit iu, String key) {
		return getIUProperty(iu, key, null);
	}
}
