package org.eclipse.equinox.internal.p2.metadata;

import java.util.Iterator;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;

public class LDAPQuery implements IQuery<Boolean> {
	private String filter;

	public LDAPQuery(String filter) {
		this.filter = filter;
	}

	public String getFilter() {
		return filter;
	}

	public IQueryResult<Boolean> perform(Iterator<Boolean> iterator) {
		throw new IllegalStateException();
	}

	public String getId() {
		return null;
	}

	public Object getProperty(String property) {
		return null;
	}

	public void setFilter(String text) {
		filter = text;
	}

	public boolean equals(Object obj) {
		if (obj instanceof LDAPQuery)
			return filter.equals(((LDAPQuery) obj).getFilter());
		return false;
	}
}
