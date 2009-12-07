/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ql;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import org.eclipse.equinox.p2.metadata.IVersionedId;

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;

import org.eclipse.equinox.internal.p2.ql.ExpressionParser;
import org.eclipse.equinox.internal.p2.ql.ItemExpression;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.p2.ql.PredicateQuery;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class TestQueryReimplementation extends AbstractProvisioningTest {

	public static class UpdateQuery extends PredicateQuery {
		private static final ItemExpression expr1;
		private static final ItemExpression expr2;

		static {
			ExpressionParser parser = new ExpressionParser();

			// This expression is used in case the updateFrom is an IInstallableUnitPatch
			//
			expr1 = parser.parsePredicate("$0 ~= updateDescriptor && ($0.id != id || $0.version < version)");

			// When updateFrom is not an IInstallableUnitPatch, we need to do one of two things depending
			// on if the current item is an InstallableUnitPatch or not.
			//
			expr2 = parser.parsePredicate("item ~= class('org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitPatch')" + //
					"? $0 ~= lifeCycle" + //
					": $0 ~= updateDescriptor && ($0.id != id || $0.version < version)");
		}

		public UpdateQuery(IInstallableUnit updateFrom) {
			super(IInstallableUnit.class, updateFrom instanceof IInstallableUnitPatch ? expr1 : expr2, new Object[] {updateFrom, IInstallableUnitPatch.class});
		}
	}

	public static class IUPropertyQuery extends PredicateQuery {
		private static final ItemExpression expr = new ExpressionParser().parsePredicate("properties[$0] == $1");

		public IUPropertyQuery(String propertyName, String propertyValue) {
			super(IInstallableUnit.class, expr, new Object[] {propertyName, propertyValue});
		}
	}

	public static class InstallableUnitQuery extends PredicateQuery {
		/**
		 * A convenience query that will match any {@link IInstallableUnit}
		 * it encounters.
		 */
		public static final PredicateQuery ANY = new PredicateQuery("");

		private static final ItemExpression idVersionQuery;
		private static final ItemExpression idRangeQuery;

		static {
			ExpressionParser parser = new ExpressionParser();
			idVersionQuery = parser.parsePredicate("($0 == null || $0 == id) && ($1 == null || $1 == version)");
			idRangeQuery = parser.parsePredicate("($0 == null || $0 == id) && ($1 == null || version ~= $1)");
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
			super(IInstallableUnit.class, idRangeQuery, new Object[] {id, range});
		}

		/**
		 * Creates a query that will match any {@link IInstallableUnit} with the given
		 * id and version.
		 * 
		 * @param id The installable unit id to match, or <code>null</code> to match any id
		 * @param version The precise version that a matching unit must have
		 */
		public InstallableUnitQuery(String id, Version version) {
			super(IInstallableUnit.class, idVersionQuery, new Object[] {id, version});
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

	/**
	 * A query that searches for {@link IInstallableUnit} instances that provide
	 * capabilities that match one or more required capabilities.
	 */
	public static class CapabilityQuery extends PredicateQuery {
		private static final ItemExpression oneCapabilityQuery;
		private static final ItemExpression anyCapabilityQuery;

		static {
			ExpressionParser parser = new ExpressionParser();
			oneCapabilityQuery = parser.parsePredicate("item ~= $1");
			anyCapabilityQuery = parser.parsePredicate("$1.exists(x | item ~= x)");
		}

		/**
		 * Creates a new query on the given required capability.
		 * @param required The required capability
		 */
		public CapabilityQuery(IRequiredCapability required) {
			super(oneCapabilityQuery, new Object[] {required});
		}

		/**
		 * Creates a new query on the given required capabilities. The installable
		 * unit must provide capabilities that match all of the given required capabilities
		 * for this query to be satisfied.
		 * @param required The required capabilities
		 */
		public CapabilityQuery(IRequiredCapability[] required) {
			super(anyCapabilityQuery, new Object[] {required});
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
		Collector c = repo.query(new UpdateQuery(a1), new Collector(), null);
		assertEquals(1, c.size());
		assertEquals(updateOfA, c.iterator().next());
	}

	public void testWithSuperiorVersion() {
		IMetadataRepository repo2 = createTestMetdataRepository(new IInstallableUnit[] {a11, a1});
		Collector c2 = repo2.query(new UpdateQuery(a1), new Collector(), null);
		assertEquals(1, c2.size());
		assertEquals(a11, c2.iterator().next());
	}

	private IInstallableUnit createIUUpdate() {
		return createIU("A", Version.create("2.1.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, MetadataFactory.createUpdateDescriptor("A", new VersionRange("[2.0.0, 2.1.0]"), 0, "update description"), null);
	}
}