package org.eclipse.equinox.internal.p2.director;

import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.*;

public class QueryableArray implements IQueryable {
	List dataSet;

	public QueryableArray(IInstallableUnit[] ius) {
		dataSet = Arrays.asList(ius);
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		return query.perform(dataSet.iterator(), collector);
	}

}
