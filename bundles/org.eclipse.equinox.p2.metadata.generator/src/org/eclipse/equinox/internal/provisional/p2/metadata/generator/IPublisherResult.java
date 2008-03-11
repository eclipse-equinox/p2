package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import java.util.Collection;
import java.util.Map;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

public interface IPublisherResult {
	public static final int MERGE_MATCHING = 0;
	public static final int MERGE_ALL_ROOT = 1;
	public static final int MERGE_ALL_NON_ROOT = 2;

	public static final String CONFIGURATION_CUS = "CONFIGURATION_CUS"; //$NON-NLS-1$

	// type markers
	public static final String ROOT = "root"; //$NON-NLS-1$
	public static final String NON_ROOT = "non_root"; //$NON-NLS-1$

	public void addIU(IInstallableUnit iu, String type);

	public void addIUs(Collection ius, String type);

	public void addFragment(String hostId, IInstallableUnit iu);

	/**
	 * Returns the IUs in this result with the given id.
	 */
	public Collection getIUs(String id, String type);

	public IInstallableUnit getIU(String id, String type);

	public Collection getFragments(String hostId);

	public Map getFragmentMap();

	public void merge(IPublisherResult result, int mode);

	// TODO not happy about having this here.  Need to figure out a better plan
	public Map getConfigData();
}
