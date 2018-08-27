/*******************************************************************************
* Copyright (c) 2009, 2017 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.*;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.updatesite.LocalUpdateSiteAction;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;

public class LocalUpdateSiteActionTest extends ActionTest {

	protected TestArtifactRepository artifactRepository = new TestArtifactRepository(getAgent());

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupPublisherResult();
		setupPublisherInfo();
	}

	@Override
	protected void insertPublisherInfoBehavior() {
		super.insertPublisherInfoBehavior();
		expect(publisherInfo.getArtifactRepository()).andReturn(artifactRepository).anyTimes();
		expect(publisherInfo.getArtifactOptions()).andReturn(IPublisherInfo.A_INDEX | IPublisherInfo.A_OVERWRITE | IPublisherInfo.A_PUBLISH).anyTimes();
		expect(publisherInfo.getAdvice((String) anyObject(), anyBoolean(), (String) anyObject(), (Version) anyObject(), (Class<IPublisherAdvice>) anyObject())).andReturn(Collections.EMPTY_LIST).anyTimes();
	}

	/**
	 * This test uses a simple site.xml (with a zipped up feature) and ensures
	 * that the metadata to unzip the feature is available.
	 * @throws Exception
	 */
	public void testUnzipTouchpointAction() throws Exception {
		File file = TestData.getFile("updatesite/site", "");
		LocalUpdateSiteAction action = new LocalUpdateSiteAction(file.getAbsolutePath(), "qualifier");
		action.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		Collection<IInstallableUnit> ius = publisherResult.getIUs("test.feature.feature.jar", null);
		assertEquals("1.0", 1, ius.size());
		IInstallableUnit iu = ius.iterator().next();
		Collection<ITouchpointData> touchpointData = iu.getTouchpointData();
		assertEquals("1.1", 1, touchpointData.size());
		Map<String, ITouchpointInstruction> instructions = touchpointData.iterator().next().getInstructions();
		Set<String> keys = instructions.keySet();
		assertEquals("1.2", 1, keys.size());
		String unzip = keys.iterator().next();
		assertEquals("1.3", "zipped", unzip);
	}
}
