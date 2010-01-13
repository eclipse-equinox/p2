package org.eclipse.equinox.internal.p2.ql;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.expression.IRepeatableIterator;
import org.eclipse.equinox.internal.p2.metadata.expression.RepeatableIterator;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ql.IQueryContext;
import org.eclipse.equinox.p2.ql.ITranslationSupport;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.osgi.service.localization.LocaleProvider;

public class QueryContext<T> implements IQueryContext<T> {

	private final IQueryable<T> queryable;

	private Map<Locale, TranslationSupport> translationSupports;

	public QueryContext(IQueryable<T> queryable) {
		this.queryable = queryable;
	}

	public QueryContext(Iterator<T> iterator) {
		final IRepeatableIterator<T> repeatable = RepeatableIterator.create(iterator);
		this.queryable = new IQueryable<T>() {
			public IQueryResult<T> query(IQuery<T> query, IProgressMonitor monitor) {
				return query.perform(repeatable.getCopy());
			}
		};
	}

	public synchronized ITranslationSupport getTranslationSupport(final Locale locale) {
		if (translationSupports == null)
			translationSupports = new HashMap<Locale, TranslationSupport>();

		TranslationSupport ts = translationSupports.get(locale);
		if (ts == null) {
			ts = new TranslationSupport();
			ts.setTranslationSource((IQueryable<IInstallableUnit>) queryable);
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
		@SuppressWarnings("unchecked")
		final Iterator<T>[] iteratorCatcher = new Iterator[1];
		queryable.query(new ContextQuery<T>() {
			public IQueryResult<T> perform(Iterator<T> iterator) {
				iteratorCatcher[0] = iterator;
				return null;
			}
		}, new NullProgressMonitor());
		return iteratorCatcher[0];
	}
}
