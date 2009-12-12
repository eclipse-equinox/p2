package org.eclipse.equinox.p2.ql;

import java.util.Iterator;
import java.util.Locale;

public interface IQueryContext {
	ITranslationSupport getTranslationSupport(Locale locale);

	Iterator iterator();
}
