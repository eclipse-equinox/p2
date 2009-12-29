package org.eclipse.equinox.p2.ql;

import java.util.Iterator;
import java.util.Locale;

public interface IQueryContext<T> {
	ITranslationSupport getTranslationSupport(Locale locale);

	Iterator<T> iterator();
}
