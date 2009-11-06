package org.eclipse.equinox.p2.metadata.query;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;

/**
 * A query matching every {@link IInstallableUnit} that is a category.
 * @since 2.0
 */
public final class FragmentQuery extends MatchQuery {
	private IUPropertyQuery query;

	public FragmentQuery() {
		query = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_FRAGMENT, null);
	}

	public boolean isMatch(Object candidate) {
		return query.isMatch(candidate);
	}

	/**
	 * Test if the {@link IInstallableUnit} is a fragment. 
	 * @param iu the element being tested.
	 * @return <tt>true</tt> if the parameter is a group.
	 */
	public static boolean isFragment(IInstallableUnit iu) {
		String value = iu.getProperty(IInstallableUnit.PROP_TYPE_FRAGMENT);
		if (value != null && (value.equals(Boolean.TRUE.toString())))
			return true;
		return false;
	}
}
