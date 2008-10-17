package org.eclipse.equinox.internal.p2.director;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.internal.provisional.p2.query.*;

public class QueryableArray implements IQueryable {
	static class IUCapability {
		final IInstallableUnit iu;
		final ProvidedCapability capability;

		public IUCapability(IInstallableUnit iu, ProvidedCapability capability) {
			this.iu = iu;
			this.capability = capability;
		}
	}

	private final List dataSet;
	private Map namedCapabilityIndex;

	public QueryableArray(IInstallableUnit[] ius) {
		dataSet = Arrays.asList(ius);
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		if (query instanceof CapabilityQuery)
			return queryCapability((CapabilityQuery) query, collector, monitor);
		return query.perform(dataSet.iterator(), collector);
	}

	private Collector queryCapability(CapabilityQuery query, Collector collector, IProgressMonitor monitor) {
		generateNamedCapabilityIndex();

		RequiredCapability[] requiredCapabilities = query.getRequiredCapabilities();
		Collection resultIUs = null;
		for (int i = 0; i < requiredCapabilities.length; i++) {
			Collection matchingIUs = findMatchingIUs(requiredCapabilities[i]);
			if (matchingIUs == null)
				return collector;
			if (resultIUs == null)
				resultIUs = matchingIUs;
			else
				resultIUs.retainAll(matchingIUs);
		}

		for (Iterator iterator = resultIUs.iterator(); iterator.hasNext();)
			collector.accept(iterator.next());

		return collector;
	}

	private Collection findMatchingIUs(RequiredCapability requiredCapability) {
		List iuCapabilities = (List) namedCapabilityIndex.get(requiredCapability.getName());
		if (iuCapabilities == null)
			return null;

		Set matchingIUs = new HashSet();
		for (Iterator iterator = iuCapabilities.iterator(); iterator.hasNext();) {
			IUCapability iuCapability = (IUCapability) iterator.next();
			if (iuCapability.capability.satisfies(requiredCapability))
				matchingIUs.add(iuCapability.iu);
		}
		return matchingIUs;
	}

	private void generateNamedCapabilityIndex() {
		if (namedCapabilityIndex != null)
			return;

		namedCapabilityIndex = new HashMap();
		for (Iterator iterator = dataSet.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();

			ProvidedCapability[] providedCapabilities = iu.getProvidedCapabilities();
			for (int i = 0; i < providedCapabilities.length; i++) {
				String name = providedCapabilities[i].getName();
				List iuCapabilities = (List) namedCapabilityIndex.get(name);
				if (iuCapabilities == null) {
					iuCapabilities = new ArrayList();
					namedCapabilityIndex.put(name, iuCapabilities);
				}
				iuCapabilities.add(new IUCapability(iu, providedCapabilities[i]));
			}
		}
	}
}
