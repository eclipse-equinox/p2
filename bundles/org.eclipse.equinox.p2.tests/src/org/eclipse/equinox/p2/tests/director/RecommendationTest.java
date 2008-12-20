/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import org.eclipse.core.runtime.IStatus;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class RecommendationTest extends AbstractProvisioningTest {
	//test name dependency over
	//test
	//check that the picker is returning something in the range
	public void testRecommendation() {
		RequiredCapability applyOn, newValue;
		applyOn = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.0, 2.0)"), null, false, false);
		newValue = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.1, 2.0)"), null, false, false);
		Recommendation r1 = new Recommendation(applyOn, newValue);

		RequiredCapability goodMatch = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.0, 2.0)"), null, false, false);
		assertEquals(true, r1.matches(goodMatch));

		RequiredCapability badNamespace = MetadataFactory.createRequiredCapability("badNamespace", "name", new VersionRange("[1.0, 2.0)"), null, false, false);
		assertEquals(false, r1.matches(badNamespace));

		RequiredCapability badName = MetadataFactory.createRequiredCapability("namespace", "badName", new VersionRange("[1.0, 2.0)"), null, false, false);
		assertEquals(false, r1.matches(badName));
	}

	public void testPicker() {
		//The IUs we will pick from
		IInstallableUnit iu1 = createIU("iu1", new Version(1, 0, 0));
		IInstallableUnit iu2 = createIU("iu2", new Version(4, 0, 0));

		//The recommendations to be used
		RequiredCapability applyOn, newValue;
		applyOn = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu1", new VersionRange("[1.0, 2.0)"), null, false, false);
		newValue = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu1", new VersionRange("[1.1, 2.0)"), null, false, false);
		Recommendation r1 = new Recommendation(applyOn, newValue);

		RequiredCapability applyOn2, newValue2;
		applyOn2 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu2", new VersionRange("[4.2, 5.0)"), null, false, false);
		newValue2 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu2", new VersionRange("[4.0, 5.0)"), null, false, false);
		Recommendation r2 = new Recommendation(applyOn2, newValue2);
		Set recommendations = new HashSet();
		recommendations.add(r1);
		recommendations.add(r2);

		Picker p = new Picker(new IInstallableUnit[] {iu1, iu2}, null);
		IInstallableUnit[][] matches = p.findInstallableUnit(null, null, new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu1", null, null, false, false)}, false);
		assertEquals(matches[1][0], iu1);

		Picker p1 = new Picker(new IInstallableUnit[] {iu1, iu2}, new RecommendationDescriptor(recommendations));
		matches = p1.findInstallableUnit(null, null, new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu1", new VersionRange("[1.0, 2.0)"), null, false, false)}, false);
		assertEquals(matches[0].length, 0);
		assertEquals(matches[1].length, 0);

		matches = p1.findInstallableUnit(null, null, new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu1", new VersionRange("[4.2, 5.0)"), null, false, false)}, false);
		assertEquals(matches[0].length, 0);
		assertEquals(matches[1].length, 0);
	}

	public void testWideningRanges() {
		//The IUs we will pick from
		IInstallableUnit iu1 = createIU("iu1", new Version(4, 0, 0));

		//Here we add recommendation that widen the range of the bundle we are looking for
		RequiredCapability applyOn2, newValue2;
		applyOn2 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu1", new VersionRange("[4.2, 5.0)"), null, false, false);
		newValue2 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu1", new VersionRange("[4.0, 5.0)"), null, false, false);
		Recommendation r2 = new Recommendation(applyOn2, newValue2);
		Set recommendations = new HashSet();
		recommendations.add(r2);

		//Check without the recommendations
		Picker p2 = new Picker(new IInstallableUnit[] {iu1}, null);
		IInstallableUnit[][] matches = p2.findInstallableUnit(null, null, new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu1", new VersionRange("[4.0, 5.0)"), null, false, false)}, false);
		assertEquals(matches[1].length, 1);

		//Check the widening works
		Picker p1 = new Picker(new IInstallableUnit[] {iu1}, new RecommendationDescriptor(recommendations));
		matches = p1.findInstallableUnit(null, null, new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "iu1", new VersionRange("[4.2, 5.0)"), null, false, false)}, false);
		assertEquals(matches[1].length, 1);

	}

	public void testRecommendationDescriptorMerge() {
		RequiredCapability applyOn1, newValue1;
		applyOn1 = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.0, 2.0)"), null, false, false);
		newValue1 = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.1, 2.0)"), null, false, false);
		Recommendation r1 = new Recommendation(applyOn1, newValue1);
		Set list1 = new HashSet();
		list1.add(r1);
		RecommendationDescriptor desc1 = new RecommendationDescriptor(list1);

		RequiredCapability applyOn2, newValue2;
		applyOn2 = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.0, 2.0)"), null, false, false);
		newValue2 = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.3, 2.0)"), null, false, false);
		Recommendation r2 = new Recommendation(applyOn2, newValue2);
		Set list2 = new HashSet();
		list2.add(r2);
		RecommendationDescriptor desc2 = new RecommendationDescriptor(list2);

		//We test that the result of the merge worked.
		assertEquals(Status.OK_STATUS, desc1.merge(desc2));
		assertEquals(r2, desc1.findRecommendation(MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.0, 2.0)"), null, false, false)));
	}

	public void testRecommendationDescriptorMergeConflict() {
		RequiredCapability applyOn1, newValue1;
		applyOn1 = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.0, 2.0)"), null, false, false);
		newValue1 = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.1, 2.0)"), null, false, false);
		Recommendation r1 = new Recommendation(applyOn1, newValue1);
		Set list1 = new HashSet();
		list1.add(r1);
		RecommendationDescriptor desc1 = new RecommendationDescriptor(list1);

		RequiredCapability applyOn2, newValue2;
		applyOn2 = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[1.0, 2.0)"), null, false, false);
		newValue2 = MetadataFactory.createRequiredCapability("namespace", "name", new VersionRange("[2.1, 3.0)"), null, false, false);
		Recommendation r2 = new Recommendation(applyOn2, newValue2);
		Set list2 = new HashSet();
		list2.add(r2);
		RecommendationDescriptor desc2 = new RecommendationDescriptor(list2);

		//We test that the result of the merge worked.
		assertEquals(IStatus.INFO, desc1.merge(desc2).getSeverity());
	}

	public void testRangeIntersection() {
		Recommendation rec = new Recommendation(null, null);
		try {
			Method m = rec.getClass().getDeclaredMethod("intersect", new Class[] {VersionRange.class, VersionRange.class});
			m.setAccessible(true);
			assertEquals(new VersionRange("[1.0.0, 2.0.0)"), m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0)"), new VersionRange("[0.1.0, 3.0.0]")}));

			assertEquals(new VersionRange("[1.1.0, 1.9.0]"), m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0)"), new VersionRange("[1.1.0, 1.9.0]")}));
			assertEquals(new VersionRange("[1.1.0, 2.0.0)"), m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0)"), new VersionRange("[1.1.0, 2.1.0]")}));
			assertEquals(new VersionRange("[1.0.0, 1.3.0]"), m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0)"), new VersionRange("[0.9.0, 1.3.0]")}));
			assertEquals(null, m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0)"), new VersionRange("[3.0.0, 4.0.0]")}));

			assertEquals(new VersionRange("(1.0.0, 2.0.0]"), m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0]"), new VersionRange("(1.0.0, 2.1.0]")}));
			assertEquals(new VersionRange("(1.0.0, 2.0.0]"), m.invoke(rec, new Object[] {new VersionRange("(1.0.0, 2.0.0]"), new VersionRange("[1.0.0, 2.1.0]")}));

			assertEquals(new VersionRange("[1.0.0, 2.0.0)"), m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0)"), new VersionRange("[1.0.0, 2.0.0]")}));
			assertEquals(new VersionRange("[1.0.0, 2.0.0)"), m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0]"), new VersionRange("[1.0.0, 2.0.0)")}));

			assertEquals(new VersionRange("[1.0.0, 2.0.0]"), m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0]"), new VersionRange("[1.0.0, 2.0.0]")}));
			assertEquals(new VersionRange("(1.0.0, 2.0.0)"), m.invoke(rec, new Object[] {new VersionRange("(1.0.0, 2.0.0)"), new VersionRange("(1.0.0, 2.0.0)")}));

			assertEquals(null, m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0)"), new VersionRange("[2.0.0, 3.0.0)")}));
			assertEquals(new VersionRange("[2.0.0, 2.0.0]"), m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0]"), new VersionRange("[2.0.0, 3.0.0)")}));
			assertEquals(null, m.invoke(rec, new Object[] {new VersionRange("[1.0.0, 2.0.0]"), new VersionRange("(2.0.0, 3.0.0)")}));
		} catch (Exception e) {
			fail("Usage of reflection failed");
		}
	}
}
