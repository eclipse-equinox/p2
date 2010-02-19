package org.eclipse.equinox.internal.p2.ql;

import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.expression.IRepeatableIterator;
import org.eclipse.equinox.internal.p2.metadata.expression.RepeatableIterator;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.ql.IQueryContext;
import org.eclipse.equinox.p2.ql.ITranslationSupport;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.osgi.service.localization.LocaleProvider;

public class QueryContext<T> implements IQueryContext<T> {

	private final IIndexProvider<T> indexProvider;

	private Map<Locale, TranslationSupport> translationSupports;

	public QueryContext(IIndexProvider<T> indexProvider) {
		this.indexProvider = indexProvider;
	}

	public QueryContext(Iterator<T> iterator) {
		final IRepeatableIterator<T> repeatable = RepeatableIterator.create(iterator);
		this.indexProvider = new IIndexProvider<T>() {
			public Iterator<T> everything() {
				return repeatable.getCopy();
			}

			public IIndex<T> getIndex(String memberName) {
				return null;
			}
		};
	}

	@SuppressWarnings("unchecked")
	public synchronized ITranslationSupport getTranslationSupport(final Locale locale) {
		if (translationSupports == null)
			translationSupports = new HashMap<Locale, TranslationSupport>();

		TranslationSupport ts = translationSupports.get(locale);
		if (ts == null) {
			ts = new TranslationSupport();
			ts.setTranslationSource((IQueryable<IInstallableUnit>) indexProvider);
			ts.setLocaleProvider(new LocaleProvider() {
				public Locale getLocale() {
					// TODO Auto-generated method stub
					return locale;
				}
			});
			translationSupports.put(locale, ts);
		}
		return ts;
	}

	public Iterator<T> iterator() {
		return indexProvider.everything();
	}
}
