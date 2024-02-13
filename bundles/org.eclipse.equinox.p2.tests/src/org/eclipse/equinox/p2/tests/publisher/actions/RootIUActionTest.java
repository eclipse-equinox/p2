/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
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
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.actions.IAdditionalInstallableUnitAdvice;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.publisher.actions.ILicenseAdvice;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.publisher.actions.IRootIUAdvice;
import org.eclipse.equinox.p2.publisher.actions.ITouchpointAdvice;
import org.eclipse.equinox.p2.publisher.actions.IUpdateDescriptorAdvice;
import org.eclipse.equinox.p2.publisher.actions.IVersionAdvice;
import org.eclipse.equinox.p2.publisher.actions.RootIUAction;
import org.eclipse.equinox.p2.publisher.actions.RootIUAdvice;
import org.eclipse.equinox.p2.publisher.actions.RootIUResultFilterAdvice;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.MatchQuery;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.TestMetadataRepository;

@SuppressWarnings("deprecation") // MatchQuery
public class RootIUActionTest extends ActionTest {
	private static final int CONTAINS_A = 1;
	private static final int CONTAINS_B = 2;
	private static final int EMPTY = 0;
	private static final int ALL = CONTAINS_A | CONTAINS_B;
	private static final String ADVICE = "advice \t\t\t"; //$NON-NLS-1$
	private static final String METADATA_REPOSITORY = "metadata repo \t\t"; //$NON-NLS-1$
	private static final String PUBLISHER_RESULT = "publisher result \t"; //$NON-NLS-1$
	protected static String iu_A = "iuA"; //$NON-NLS-1$
	protected static String iu_B = "iuB"; //$NON-NLS-1$

	private IMetadataRepository metadataRepository;
	private String rootIU = "sdk"; //$NON-NLS-1$
	private Version versionArg = Version.create("3.4.0.i0305"); //$NON-NLS-1$
	private Collection<IRootIUAdvice> rootIUAdviceCollection;

	public void testNullAdvice() throws Exception {
		debug("\n**********************************"); //$NON-NLS-1$
		debug(" null advice test"); //$NON-NLS-1$
		setupMetadataRepository(EMPTY);
		setupPublisherResult(EMPTY);
		setupPublisherInfo();
		testAction = new RootIUAction(rootIU, versionArg, rootIU);

		assertEquals(Status.OK_STATUS, testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor()));
		confirmResultRequired(EMPTY);
		cleanup();
	}

	public void testNullRepo() throws Exception {
		debug("\n**********************************"); //$NON-NLS-1$
		debug(" null repo test"); //$NON-NLS-1$
		setupAdvice(CONTAINS_A);
		setupPublisherResult(EMPTY);
		setupPublisherInfo();
		testAction = new RootIUAction(rootIU, versionArg, rootIU);

		assertEquals(Status.OK_STATUS, testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor()));
		confirmResultRequired(EMPTY);
		cleanup();
	}

	public void testIUAdviceAMetdataAResultB() throws Exception {
		debug("\n**********************************"); //$NON-NLS-1$
		debug(" advice (as iu) A"); //$NON-NLS-1$
		setupAdvice(EMPTY);
		Collection<Object> iuCollection = new ArrayList<>();
		iuCollection.add(mockIU(iu_A, null));
		rootIUAdviceCollection.add(new RootIUAdvice(iuCollection));
		setupMetadataRepository(CONTAINS_A);
		setupPublisherResult(EMPTY);
		setupPublisherInfo();
		testAction = new RootIUAction(rootIU, versionArg, rootIU);

		assertEquals(Status.OK_STATUS, testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor()));
		confirmResultRequired(CONTAINS_A);
		cleanup();
	}

	//	@Test
	//	public void testIUAdviceAAdviceAMetadataAResultB() throws Exception {
	//		debug("\n**********************************"); //$NON-NLS-1$
	//		debug(" advice (as iu and String) A"); //$NON-NLS-1$
	//		setupAdvice(CONTAINS_A);
	//		Collection iuCollection = new ArrayList();
	//		iuCollection.add(mockIU(iu_A, null));
	//		rootIUAdviceCollection.add(new RootIUAdvice(iuCollection));
	//		setupMetadataRepository(CONTAINS_A);
	//		setupPublisherResult(EMPTY);
	//		setupPublisherInfo();
	//		testAction = new RootIUAction(rootIU, versionArg, rootIU);
	//
	//		assertEquals(Status.OK_STATUS, testAction.perform(publisherInfo, publisherResult));
	//		confirmResultRequired(CONTAINS_A);
	//		cleanup();
	//	} //TODO: commented out while we determine bug: (bug not entered due to bug in reporting bugs)

	public void testEmpty() throws Exception {
		setupAndRunRootIUTest(EMPTY, EMPTY, EMPTY);
	}

	public void testAdviceA() throws Exception {
		setupAndRunRootIUTest(CONTAINS_A, CONTAINS_A, EMPTY);
	}

	public void testAdviceAll() throws Exception {
		setupAndRunRootIUTest(ALL, ALL, EMPTY);
	}

	public void testMetadataA() throws Exception {
		setupAndRunRootIUTest(EMPTY, CONTAINS_A, EMPTY);
	}

	public void testMetadataAll() throws Exception {
		setupAndRunRootIUTest(EMPTY, ALL, EMPTY);
	}

	public void testAdviceAMetadataB() throws Exception {
		setupAndRunRootIUTest(CONTAINS_A, CONTAINS_B, EMPTY);
	}

	public void testResultA() throws Exception {
		setupAndRunRootIUTest(EMPTY, EMPTY, CONTAINS_A);
	}

	public void testResultAll() throws Exception {
		setupAndRunRootIUTest(EMPTY, EMPTY, ALL);
	}

	public void testAdviceAResultB() throws Exception {
		setupAndRunRootIUTest(CONTAINS_A, EMPTY, CONTAINS_B);
	}

	public void testAdviceAllResultAll() throws Exception {
		setupAndRunRootIUTest(ALL, EMPTY, ALL);
	}

	public void testAdviceAllMetadataAResultB() throws Exception {
		setupAndRunRootIUTest(ALL, CONTAINS_A, CONTAINS_B);
	}

	public void testAdviceAllMetadataAllResultAll() throws Exception {
		setupAndRunRootIUTest(ALL, ALL, ALL);
	}

	public void testResultFilterAdviceAllResultAll() throws Exception {
		setupAndrunRootFilterIUTest(ALL, EMPTY, ALL);
	}

	public void testResultFilterAdviceAResultAll() throws Exception {
		setupAndrunRootFilterIUTest(CONTAINS_A, EMPTY, ALL);
	}

	public void testResultFilterAdviceBResultAll() throws Exception {
		setupAndrunRootFilterIUTest(CONTAINS_B, EMPTY, ALL);
	}

	public void testResultFilterAdviceEmptyResultAll() throws Exception {
		setupAndrunRootFilterIUTest(EMPTY, EMPTY, ALL);
	}

	public void testResultFilterAdviceAllMetadataAll() throws Exception {
		setupAndrunRootFilterIUTest(ALL, EMPTY, ALL);
	}

	public void testResultFilterAdviceAMetadataAll() throws Exception {
		setupAndrunRootFilterIUTest(CONTAINS_A, EMPTY, ALL);
	}

	public void testResultFilterAdviceBMetadataAll() throws Exception {
		setupAndrunRootFilterIUTest(CONTAINS_B, EMPTY, ALL);
	}

	public void testResultFilterAdviceEmptyMetadataAll() throws Exception {
		setupAndrunRootFilterIUTest(EMPTY, EMPTY, ALL);
	}

	public void testResultFilterAdviceAllMetadataBResultA() throws Exception {
		setupAndrunRootFilterIUTest(ALL, CONTAINS_B, CONTAINS_A);
	}

	public void testResultFilterAdviceBMetadataAResultB() throws Exception {
		setupAndrunRootFilterIUTest(ALL, CONTAINS_A, CONTAINS_B);
	}

	private void setupAndRunRootIUTest(int advice, int metadataRepository, int publisherResult) {
		debug("\n**********************************"); //$NON-NLS-1$
		debug("RootIUTest"); //$NON-NLS-1$
		setupAdvice(advice);
		setupMetadataRepository(metadataRepository);
		setupPublisherResult(publisherResult);
		setupPublisherInfo();
		debug(toResultString(ADVICE, advice));
		debug(toResultString(METADATA_REPOSITORY, metadataRepository));
		debug(toResultString(PUBLISHER_RESULT, publisherResult));
		testAction = new RootIUAction(rootIU, versionArg, rootIU);

		assertEquals(Status.OK_STATUS, testAction.perform(publisherInfo, this.publisherResult, new NullProgressMonitor()));
		confirmResultRequired(advice & metadataRepository | advice & publisherResult);
		cleanup();
	}

	private void setupAndrunRootFilterIUTest(int advice, int metadataRepository, int publisherResult) {
		debug("\n**********************************"); //$NON-NLS-1$
		debug("RootIUActionTest"); //$NON-NLS-1$
		setupFilterAdvice(advice);
		setupMetadataRepository(metadataRepository);
		setupPublisherResult(publisherResult);
		setupPublisherInfo();

		debug(toResultString(ADVICE, advice));
		debug(toResultString(METADATA_REPOSITORY, metadataRepository));
		debug(toResultString(PUBLISHER_RESULT, publisherResult));
		testAction = new RootIUAction(rootIU, versionArg, rootIU);

		assertEquals(Status.OK_STATUS, testAction.perform(publisherInfo, this.publisherResult, new NullProgressMonitor()));
		confirmResultRequired(advice & publisherResult);
		cleanup();
	}

	private void setupFilterAdvice(int testSpec) {
		IQuery<IInstallableUnit> query = null;
		rootIUAdviceCollection = new ArrayList<>();
		if ((testSpec & CONTAINS_A) > 0) {
			query = new MatchQuery<>() {
				@Override
				public boolean isMatch(IInstallableUnit candidate) {
					if (candidate.getId().equals(iu_A))
						return true;
					return false;
				}
			};
			rootIUAdviceCollection.add(new RootIUResultFilterAdvice(query));
		}
		if ((testSpec & CONTAINS_B) > 0) {
			query = new MatchQuery<>() {
				@Override
				public boolean isMatch(IInstallableUnit candidate) {
					if (candidate.getId().equals(iu_B))
						return true;
					return false;
				}
			};
			rootIUAdviceCollection.add(new RootIUResultFilterAdvice(query));
		}
		if ((testSpec & EMPTY) > 0) {
			query = new MatchQuery<>() {
				@Override
				public boolean isMatch(IInstallableUnit candidate) {
					return false;
				}
			};
			rootIUAdviceCollection.add(new RootIUResultFilterAdvice(query));
		}
	}

	private void confirmResultRequired(int testSpec) {
		// checks that the results has a non root iu with required
		// capabilities from the publisher result
		ArrayList<IInstallableUnit> ius = new ArrayList<>(publisherResult.getIUs(rootIU, IPublisherResult.NON_ROOT));
		assertTrue(ius.size() == 1);
		IInstallableUnit iu = ius.get(0);
		assertTrue(iu != null);
		assertTrue(iu.getVersion().equals(versionArg));
		Collection<IRequirement> required = iu.getRequirements();
		if ((testSpec & EMPTY) > 0)
			assertEquals(required.size(), 0);
		String confirmedIUs = ""; //$NON-NLS-1$
		int numConfirmed = 0;

		if ((testSpec & CONTAINS_A) > 0) {
			assertTrue(contains(required, iu_A));
			confirmedIUs += iu_A + ' ';
			numConfirmed++;
		}
		if ((testSpec & CONTAINS_B) > 0) {
			assertTrue(contains(required, iu_B));
			confirmedIUs += iu_B;
			numConfirmed++;
		}
		if (numConfirmed != required.size()) {
			debug("Not all required ius present / accounted for."); //$NON-NLS-1$
			fail();
		}
		if (confirmedIUs.length() > 0)
			debug("Confirmed \t\t " + confirmedIUs); //$NON-NLS-1$
		else
			debug("Confirmed \t\t  Empty"); //$NON-NLS-1$
	}

	private boolean contains(Collection<IRequirement> required, String iu) {
		for (IRequirement requirement : required) {
			IRequiredCapability req = (IRequiredCapability) requirement;
			if (req.getName().equalsIgnoreCase(iu))
				return true;
		}
		return false;
	}

	public void setupPublisherResult(int testSpec) {
		super.setupPublisherResult();
		Collection<IInstallableUnit> ius = new ArrayList<>();
		if ((testSpec & CONTAINS_A) > 0) {
			ius.add(mockIU(iu_A, null));
		}
		if ((testSpec & CONTAINS_B) > 0) {
			ius.add(mockIU(iu_B, null));
		}
		publisherResult.addIUs(ius, IPublisherResult.ROOT);
	}

	private void setupMetadataRepository(int testSpec) {
		ArrayList<IInstallableUnit> repoContents = new ArrayList<>();
		if ((testSpec & CONTAINS_A) > 0) {
			repoContents.add(mockIU(iu_A, null));
		}
		if ((testSpec & CONTAINS_B) > 0) {
			repoContents.add(mockIU(iu_B, null));
		}

		IInstallableUnit[] ius = repoContents.toArray(new IInstallableUnit[repoContents.size()]);
		metadataRepository = new TestMetadataRepository(getAgent(), ius);
	}

	public void setupAdvice(int testSpec) {
		Collection<Object> publishIUs = new ArrayList<>();
		if ((testSpec & CONTAINS_A) > 0)
			publishIUs.add(iu_A);
		if ((testSpec & CONTAINS_B) > 0)
			publishIUs.add(iu_B);
		rootIUAdviceCollection = new ArrayList<>();
		rootIUAdviceCollection.add(new RootIUAdvice(publishIUs));
	}

	@Override
	public void insertPublisherInfoBehavior() {
		when(publisherInfo.getAdvice(null, false, rootIU, versionArg, ICapabilityAdvice.class))
				.thenReturn(new ArrayList<>());
		when(publisherInfo.getAdvice(null, true, null, null, IRootIUAdvice.class)).thenReturn(rootIUAdviceCollection);
		when(publisherInfo.getAdvice(null, true, null, null, IVersionAdvice.class)).thenReturn(null);
		when(publisherInfo.getAdvice(null, false, rootIU, versionArg, ITouchpointAdvice.class))
				.thenReturn(new ArrayList<>());
		when(publisherInfo.getAdvice(null, false, rootIU, versionArg, IUpdateDescriptorAdvice.class))
				.thenReturn(new ArrayList<>());
		when(publisherInfo.getAdvice(null, false, rootIU, versionArg, IPropertyAdvice.class))
				.thenReturn(new ArrayList<>());
		when(publisherInfo.getAdvice(null, false, rootIU, versionArg, IAdditionalInstallableUnitAdvice.class))
				.thenReturn(new ArrayList<>());
		when(publisherInfo.getAdvice(null, true, rootIU, versionArg, ILicenseAdvice.class))
				.thenReturn(new ArrayList<>());
		when(publisherInfo.getMetadataRepository()).thenReturn(metadataRepository);
		when(publisherInfo.getContextMetadataRepository()).thenReturn(null);
	}

	@Override
	public void cleanup() {
		super.cleanup();
		rootIUAdviceCollection = null;
		if (metadataRepository != null) {
			metadataRepository.removeAll();
			metadataRepository = null;
		}
	}

	private String toResultString(String setup, int arg) {
		return setup + toArgString(arg);
	}

	private String toArgString(int testSpec) {
		if (testSpec == EMPTY)
			return " Empty"; //$NON-NLS-1$
		String result = " "; //$NON-NLS-1$
		if ((testSpec & CONTAINS_A) > 0)
			result += iu_A;
		if ((testSpec & CONTAINS_B) > 0)
			result += " " + iu_B; //$NON-NLS-1$
		return result;
	}
}
