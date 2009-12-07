package org.eclipse.equinox.internal.p2.metadata;

import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.p2.metadata.query.IQuery;

public class LDAPQuery implements IQuery {
	private String filter;

	public LDAPQuery(String filter) {
		this.filter = filter;
	}

	public String getFilter() {
		return filter;
	}

	public Collector perform(Iterator iterator, Collector result) {
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
