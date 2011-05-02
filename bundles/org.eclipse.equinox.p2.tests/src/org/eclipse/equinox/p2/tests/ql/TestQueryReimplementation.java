/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ql;

import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class TestQueryReimplementation extends AbstractProvisioningTest {

	public static class UpdateQuery extends ExpressionMatchQuery {
		private static final IExpression expr1;
		private static final IExpression expr2;

		static {
			IExpressionParser parser = ExpressionUtil.getParser();

			// This expression is used in case the updateFrom is an IInstallableUnitPatch
			//
			expr1 = parser.parse("$0 ~= updateDescriptor && ($0.id != id || $0.version < version)");

			// When updateFrom is not an IInstallableUnitPatch, we need to do one of two things depending
			// on if the current item is an InstallableUnitPatch or not.
			//
			expr2 = parser.parse("this ~= class('org.eclipse.equinox.p2.metadata.IInstallableUnitPatch')" + //
					"? $0 ~= lifeCycle" + //
					": $0 ~= updateDescriptor && ($0.id != id || $0.version < version)");
		}

		public UpdateQuery(IInstallableUnit updateFrom) {
			super(IInstallableUnit.class, ExpressionUtil.getFactory().matchExpression(updateFrom instanceof IInstallableUnitPatch ? expr1 : expr2, updateFrom, IInstallableUnitPatch.class));
		}
	}

	public static class IUPropertyQuery extends ExpressionMatchQuery {
		private static final IExpression expr = ExpressionUtil.getParser().parse("properties[$0] == $1");

		public IUPropertyQuery(String propertyName, String propertyValue) {
			super(IInstallableUnit.class, ExpressionUtil.getFactory().matchExpression(expr, propertyName, propertyValue));
		}
	}

	public static class InstallableUnitQuery extends ExpressionMatchQuery {
		/**
		 * A convenience query that will match any {@link IInstallableUnit}
		 * it encounters.
		 */
		public static final IQuery<IInstallableUnit> ANY = QueryUtil.createMatchQuery("");

		private static final IExpression idVersionQuery;
		private static final IExpression idRangeQuery;

		static {
			IExpressionParser parser = ExpressionUtil.getParser();
			idVersionQuery = parser.parse("($0 == null || $0 == id) && ($1 == null || $1 == version)");
			idRangeQuery = parser.parse("($0 == null || $0 == id) && ($1 == null || version ~= $1)");
		}

		/**
		 * Creates a query that will match any {@link IInstallableUnit} with the given
		 * id, regardless of version.
		 * 
		 * @param id The installable unit id to match, or <code>null</code> to match any id
		 */
		public InstallableUnitQuery(String id) {
			this(id, (Version) null);
		}

		/**
		 * Creates a query that will match any {@link IInstallableUnit} with the given
		 * id, and whose version falls in the provided range.
		 * 
		 * @param id The installable unit id to match, or <code>null</code> to match any id
		 * @param range The version range to match
		 */
		public InstallableUnitQuery(String id, VersionRange range) {
			super(IInstallableUnit.class, ExpressionUtil.getFactory().matchExpression(idRangeQuery, id, range));
		}

		/**
		 * Creates a query that will match any {@link IInstallableUnit} with the given
		 * id and version.
		 * 
		 * @param id The installable unit id to match, or <code>null</code> to match any id
		 * @param version The precise version that a matching unit must have
		 */
		public InstallableUnitQuery(String id, Version version) {
			super(IInstallableUnit.class, ExpressionUtil.getFactory().matchExpression(idVersionQuery, id, version));
		}

		/**
		 * Creates a query that will match any {@link IInstallableUnit} with the given
		 * id and version.
		 * 
		 * @param versionedId The precise id/version combination that a matching unit must have
		 */
		public InstallableUnitQuery(IVersionedId versionedId) {
			this(versionedId.getId(), versionedId.getVersion());
		}
	}

	private IInstallableUnit a1;
	private IInstallableUnit updateOfA;
	private IInstallableUnit a11;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		a1 = createIU("A", Version.create("2.0.0"));
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor("A", new VersionRange("[2.0.0, 2.0.0]"), 0, "update description");
		updateOfA = createIU("UpdateA", Version.createOSGi(1, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);
		a11 = createIUUpdate();
	}

	public void testUpdateWithDifferentId() {
		IMetadataRepository repo = createTestMetdataRepository(new IInstallableUnit[] {a1, updateOfA});
		IQueryResult c = repo.query(new UpdateQuery(a1), null);
		assertEquals(1, queryResultSize(c));
		assertEquals(updateOfA, c.iterator().next());
	}

	public void testWithSuperiorVersion() {
		IMetadataRepository repo2 = createTestMetdataRepository(new IInstallableUnit[] {a11, a1});
		IQueryResult c2 = repo2.query(new UpdateQuery(a1), null);
		assertEquals(1, queryResultSize(c2));
		assertEquals(a11, c2.iterator().next());
	}

	private IInstallableUnit createIUUpdate() {
		return createIU("A", Version.create("2.1.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, MetadataFactory.createUpdateDescriptor("A", new VersionRange("[2.0.0, 2.1.0]"), 0, "update description"), null);
	}
}