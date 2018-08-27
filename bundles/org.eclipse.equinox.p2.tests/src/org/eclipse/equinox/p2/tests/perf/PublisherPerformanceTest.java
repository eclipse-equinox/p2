/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.perf;

import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * Performance tests for the p2 publisher
 */
public class PublisherPerformanceTest extends ProvisioningPerformanceTest {
	private static final int REPEATS = 5;

	public void testQueryPublisherResult() {
		final int IU_COUNT = 3000;
		new PerformanceTestRunner() {
			@SuppressWarnings("unchecked")
			IQuery<IInstallableUnit>[] queries = new IQuery[IU_COUNT];
			PublisherResult result;

			@Override
			protected void setUp() {
				IInstallableUnit[] ius = new IInstallableUnit[IU_COUNT];
				result = new PublisherResult();
				for (int i = 0; i < ius.length; i++) {
					ius[i] = generateIU(i);
					result.addIU(ius[i], IPublisherResult.ROOT);
					queries[i] = QueryUtil.createIUQuery(ius[i].getId(), ius[i].getVersion());
				}
			}

			@Override
			protected void test() {
				for (int i = 0; i < queries.length; i++) {
					result.query(queries[i], null);
				}
			}
		}.run(this, "Test query PublisherResult for " + IU_COUNT + " ius", REPEATS, 10);
	}

	public void testLimitQueryPublisherResult() {
		final int IU_COUNT = 3000;
		new PerformanceTestRunner() {
			@SuppressWarnings("unchecked")
			IQuery<IInstallableUnit>[] queries = new IQuery[IU_COUNT];
			PublisherResult result;

			@Override
			protected void setUp() {
				IInstallableUnit[] ius = new IInstallableUnit[IU_COUNT];
				result = new PublisherResult();
				for (int i = 0; i < ius.length; i++) {
					ius[i] = generateIU(i);
					result.addIU(ius[i], IPublisherResult.ROOT);
					queries[i] = QueryUtil.createLimitQuery(QueryUtil.createIUQuery(ius[i].getId(), ius[i].getVersion()), 1);
				}
			}

			@Override
			protected void test() {
				for (int i = 0; i < queries.length; i++) {
					result.query(queries[i], null);
				}
			}
		}.run(this, "Test query PublisherResult for " + IU_COUNT + " ius", REPEATS, 10);
	}
}
