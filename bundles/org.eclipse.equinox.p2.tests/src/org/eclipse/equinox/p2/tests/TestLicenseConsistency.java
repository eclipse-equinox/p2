/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * Tests that licenses in the latest release repository are consistent with the
 * platform feature license. Note this test isn't intended to be included in
 * automated tests. It produces a report on stdout that can be used to identify
 * features with inconsistent feature licenses.
 */
public class TestLicenseConsistency extends AbstractProvisioningTest {
	public void testLicenses() throws URISyntaxException, ProvisionException, OperationCanceledException {
		URI repoLocation = new URI("https://download.eclipse.org/releases/latest");
		IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(repoLocation, null);
		IQueryResult<IInstallableUnit> allFeatures = repo.query(QueryUtil.createIUGroupQuery(), null);
		IQueryResult<IInstallableUnit> platform = allFeatures.query(QueryUtil.createIUQuery("org.eclipse.platform.feature.group"), null);
		assertFalse(allFeatures.isEmpty());
		assertFalse(platform.isEmpty());
		IInstallableUnit platformFeature = platform.iterator().next();
		ILicense platformLicense = platformFeature.getLicenses(null).iterator().next();

		List<IInstallableUnit> noLicense = new ArrayList<>();
		List<IInstallableUnit> extraLicense = new ArrayList<>();
		List<IInstallableUnit> goodLicense = new ArrayList<>();
		List<IInstallableUnit> badLicense = new ArrayList<>();
		checkLicenses(platformLicense, allFeatures, goodLicense, badLicense, noLicense, extraLicense);

		printReport(goodLicense, badLicense, noLicense, extraLicense);

	}

	private void printReport(List<IInstallableUnit> goodLicense, List<IInstallableUnit> badLicense, List<IInstallableUnit> noLicense, List<IInstallableUnit> extraLicense) {
		String SPACER = "\n=======================";
		System.out.println("\n\nSummary:" + SPACER);
		System.out.println("Features with conforming license: " + goodLicense.size());
		System.out.println("Features with different license: " + badLicense.size());
		System.out.println("Features with no license: " + noLicense.size());
		System.out.println("Features with extra licenses: " + extraLicense.size());
		System.out.println("=======================");

		System.out.println("\n\nDetails:" + SPACER);

		System.out.println("Features with no license:" + SPACER);
		for (IInstallableUnit unit : sort(noLicense)) {
			System.out.println(unit.getId() + ' ' + unit.getVersion());
		}

		System.out.println("\n\nFeatures with different license:" + SPACER);
		for (IInstallableUnit unit : sort(badLicense)) {
			System.out.println(unit.getId() + ' ' + unit.getVersion());
		}

		System.out.println("\n\nFeatures with matching license:" + SPACER);
		for (IInstallableUnit unit : sort(goodLicense)) {
			System.out.println(unit.getId() + ' ' + unit.getVersion());
		}

	}

	private List<IInstallableUnit> sort(List<IInstallableUnit> noLicense) {
		noLicense.sort(null);
		return noLicense;
	}

	private void checkLicenses(ILicense platformLicense, IQueryResult<IInstallableUnit> allFeatures, List<IInstallableUnit> goodLicense, List<IInstallableUnit> badLicense, List<IInstallableUnit> noLicense, List<IInstallableUnit> extraLicense) {
		for (IInstallableUnit feature : allFeatures.toUnmodifiableSet()) {
			//ignore groups that are not features
			if (!feature.getId().endsWith(".feature.group")) {
				continue;
			}
			Collection<ILicense> licenses = feature.getLicenses(null);
			if (licenses.isEmpty()) {
				noLicense.add(feature);
				continue;
			}
			if (licenses.size() != 1) {
				extraLicense.add(feature);
				continue;
			}
			ILicense featureLicense = licenses.iterator().next();
			if (!platformLicense.getUUID().equals(featureLicense.getUUID())) {
				badLicense.add(feature);
				//				if (featureLicense.getBody().length() < 100) {
				//					System.out.println(feature.getId() + " license: " + featureLicense.getBody());
				//				}
				continue;
			}
			goodLicense.add(feature);
		}
	}
}
