package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

public class PublisherResult implements IPublisherResult {
	// type markers
	public static final String ROOT = "root"; //$NON-NLS-1$
	public static final String NON_ROOT = "non_root"; //$NON-NLS-1$
	public static final String FRAGMENT = "fragment"; //$NON-NLS-1$

	// The set of top level IUs
	final Map rootIUs = new HashMap();

	// The set of internal and leaf IUs
	final Map nonRootIUs = new HashMap();

	// Map of IU id to a set of fragments for that IU 
	final Map fragmentMap = new HashMap();

	// map of os, ws, arch to ConfigData objects
	private final Map configData = new HashMap(11);

	public void addIU(IInstallableUnit iu, String type) {
		if (type == ROOT)
			addIU(rootIUs, iu.getId(), iu);
		if (type == NON_ROOT)
			addIU(nonRootIUs, iu.getId(), iu);
	}

	public void addIUs(Collection ius, String type) {
		for (Iterator i = ius.iterator(); i.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) i.next();
			addIU(iu, type);
		}
	}

	public void addFragment(String hostId, IInstallableUnit iu) {
		addIU(fragmentMap, hostId, iu);
	}

	public Map getFragmentMap() {
		return fragmentMap;
	}

	public Collection getFragments(String hostId) {
		return Arrays.asList((IInstallableUnit[]) fragmentMap.get(hostId));
	}

	private void addIU(Map map, String id, IInstallableUnit iu) {
		IInstallableUnit[] ius = (IInstallableUnit[]) map.get(id);
		if (ius == null) {
			ius = new IInstallableUnit[] {iu};
			map.put(id, ius);
		} else {
			IInstallableUnit[] newIUs = new IInstallableUnit[ius.length + 1];
			System.arraycopy(ius, 0, newIUs, 0, ius.length);
			newIUs[ius.length] = iu;
			map.put(id, newIUs);
		}
	}

	/**
	 * Returns all IUs generated during this execution of the generator.
	 */
	public Map getGeneratedIUs(String type) {
		if (type == null) {
			HashMap all = new HashMap();
			all.putAll(rootIUs);
			all.putAll(nonRootIUs);
			return all;
		}
		if (type == ROOT)
			return rootIUs;
		if (type == NON_ROOT)
			return nonRootIUs;
		throw new IllegalArgumentException("Invalid IU type: " + type); //$NON-NLS-1$
	}

	// TODO this method really should not be needed as it just returns the first
	// matching IU non-deterministically.
	public IInstallableUnit getIU(String id, String type) {
		if (type == null || type == ROOT) {
			IInstallableUnit[] ius = (IInstallableUnit[]) rootIUs.get(id);
			if (ius != null && ius.length > 0)
				return ius[0];
		}
		if (type == null || type == NON_ROOT) {
			IInstallableUnit[] ius = (IInstallableUnit[]) nonRootIUs.get(id);
			if (ius != null && ius.length > 0)
				return ius[0];
		}
		return null;
	}

	/**
	 * Returns the IUs in this result with the given id.
	 */
	public Collection getIUs(String id, String type) {
		if (type == null) {
			ArrayList result = new ArrayList();
			result.addAll(id == null ? flatten(rootIUs.values()) : Arrays.asList((Object[]) rootIUs.get(id)));
			result.addAll(id == null ? flatten(nonRootIUs.values()) : Arrays.asList((Object[]) nonRootIUs.get(id)));
			return result;
		}
		if (type == ROOT)
			return id == null ? flatten(rootIUs.values()) : Arrays.asList((Object[]) rootIUs.get(id));
		if (type == NON_ROOT)
			return id == null ? flatten(nonRootIUs.values()) : Arrays.asList((Object[]) nonRootIUs.get(id));
		return null;
	}

	private List flatten(Collection values) {
		ArrayList result = new ArrayList();
		for (Iterator i = values.iterator(); i.hasNext();) {
			IInstallableUnit[] ius = (IInstallableUnit[]) i.next();
			for (int j = 0; j < ius.length; j++)
				result.add(ius[j]);
		}
		return result;
	}

	public Map getConfigData() {
		return configData;
	}

	public void merge(IPublisherResult result, int mode) {
		// merge non-conditional pieces
		fragmentMap.putAll(result.getFragmentMap());
		configData.putAll(result.getConfigData());

		if (mode == MERGE_MATCHING) {
			addIUs(result.getIUs(null, ROOT), ROOT);
			addIUs(result.getIUs(null, NON_ROOT), NON_ROOT);
		} else if (mode == MERGE_ALL_ROOT) {
			addIUs(result.getIUs(null, ROOT), ROOT);
			addIUs(result.getIUs(null, NON_ROOT), ROOT);
		} else if (mode == MERGE_ALL_NON_ROOT) {
			addIUs(result.getIUs(null, ROOT), NON_ROOT);
			addIUs(result.getIUs(null, NON_ROOT), NON_ROOT);
		}
	}
}
