package org.eclipse.equinox.internal.p2.director.app;

import java.util.HashMap;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;

public class LatestIUVersionCollector extends Collector {
	private HashMap uniqueIds = new HashMap();

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		if (!(match instanceof IInstallableUnit))
			return true;
		IInstallableUnit iu = (IInstallableUnit) match;
		// Look for the latest element
		Object matchElement = uniqueIds.get(iu.getId());
		if (matchElement == null || iu.getVersion().compareTo(getIU(matchElement).getVersion()) > 0) {
			if (matchElement != null)
				getList().remove(matchElement);
			uniqueIds.put(iu.getId(), matchElement);
			matchElement = makeDefaultElement(iu);
			return super.accept(matchElement);
		}
		return true;
	}

	private Object makeDefaultElement(IInstallableUnit iu) {
		return iu;
	}

	protected IInstallableUnit getIU(Object matchElement) {
		if (matchElement instanceof IInstallableUnit)
			return (IInstallableUnit) matchElement;
		return null;
	}
}
