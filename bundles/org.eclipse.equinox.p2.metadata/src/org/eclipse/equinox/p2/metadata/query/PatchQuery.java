package org.eclipse.equinox.p2.metadata.query;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;

/**
 * A query matching every {@link IInstallableUnit} that is a patch. 
 * @since 2.0
 */
public final class PatchQuery extends MatchQuery {
	IUPropertyQuery query;

	public PatchQuery() {
		query = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_PATCH, Boolean.TRUE.toString());
	}

	public boolean isMatch(Object candidate) {
		return query.isMatch(candidate);
	}

	/**
	 * Test if the {@link IInstallableUnit} is a patch. 
	 * @param iu the element being tested.
	 * @return <tt>true</tt> if the parameter is a patch.
	 */
	public static boolean isPatch(IInstallableUnit iu) {
		String value = iu.getProperty(IInstallableUnit.PROP_TYPE_PATCH);
		if (value != null && (value.equals(Boolean.TRUE.toString())))
			return true;
		return false;
	}
}
