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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IRootFilesAdvice;
import org.eclipse.equinox.p2.publisher.actions.ITouchpointAdvice;
import org.eclipse.equinox.p2.publisher.actions.RootFilesAction;
import org.eclipse.equinox.p2.publisher.actions.RootFilesAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;

public class RootFilesActionTest extends ActionTest {
	private static final int INCLUDES_ROOT = 1;
	private static final int ARTIFACT_REPO = 2;
	private static final int INCLUDES_FILES = 4;
	private static final int EXCLUDE_INCLUDED = 8;
	private static final int EXCLUDES_UNUSED = 16;
	private static final int ALL = 31;

	protected static String topArg = "sdk.rootfiles.win32.win32.x86"; //$NON-NLS-1$
	protected String rootExclusions = null;
	protected IArtifactRepository artifactRepository;
	protected String idArg = "sdk"; //$NON-NLS-1$
	protected Version versionArg = Version.create("3.4.0.i0305"); //$NON-NLS-1$
	private File root = new File(TestActivator.getTestDataFolder(), "RootFilesActionTest/eclipse"); //$NON-NLS-1$
	private File[] includedFiles;
	private File[] excludedFiles;
	private Collection<IRootFilesAdvice> adviceCollection;
	private String FILE1 = "level1/level2/file1.jar"; //$NON-NLS-1$
	private String FILE2 = "level1/level2/level3/file1.jar"; //$NON-NLS-1$
	private int testArg;

	public void testAll() throws Exception {
		for (int i = 0; i < ALL; i++) {
			setupTestCase(i);
			setupPublisherInfo();
			setupPublisherResult();
			testAction = new RootFilesAction(publisherInfo, idArg, versionArg, flavorArg);
			assertEquals(Status.OK_STATUS, testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor()));
			verifyRepositoryContents(i);
			cleanup();
		}
	}

	@Override
	public void insertPublisherInfoBehavior() {
		when(publisherInfo.getArtifactRepository()).thenReturn(artifactRepository);
		when(publisherInfo.getArtifactOptions())
				.thenReturn(IPublisherInfo.A_INDEX | IPublisherInfo.A_OVERWRITE | IPublisherInfo.A_PUBLISH);
		when(publisherInfo.getAdvice(configSpec, true, null, null, IRootFilesAdvice.class))
				.thenReturn(adviceCollection);
		when(publisherInfo.getAdvice(configSpec, false, flavorArg + topArg, versionArg, ITouchpointAdvice.class))
				.thenReturn(null);
	}

	private void setupTestCase(int testArg) throws Exception {
		this.testArg = testArg;
		adviceCollection = new ArrayList<>();
		topLevel = AbstractPublisherAction.getArrayFromString(topArg, COMMA_SEPARATOR);

		if ((testArg & ARTIFACT_REPO) > 0)
			artifactRepository = new TestArtifactRepository(getAgent());

		if ((testArg & INCLUDES_FILES) > 0) {
			adviceCollection.add(new RootFilesAdvice(null, root.listFiles(), null, configSpec));
		}

		if ((testArg & INCLUDES_ROOT) > 0) {
			adviceCollection.add(new RootFilesAdvice(root, null, null, configSpec));
		}

		if (((testArg & EXCLUDE_INCLUDED) > 0) && includedFiles != null && includedFiles.length > 1) {
			excludedFiles = new File[1];
			excludedFiles[0] = includedFiles[0];
			adviceCollection.add(new RootFilesAdvice(null, null, excludedFiles, configSpec));
		}

		if ((testArg & EXCLUDES_UNUSED) > 0) {
			excludedFiles = new File[1];
			excludedFiles[0] = new File(root, "/eclipse/notHere"); //$NON-NLS-1$
			adviceCollection.add(new RootFilesAdvice(null, null, excludedFiles, configSpec));
		}
		setupPublisherInfo();
	}

	private void verifyRepositoryContents(int arg) throws Exception {
		boolean artifactRepo = (arg & ARTIFACT_REPO) > 0;
		boolean includeFiles = (arg & INCLUDES_FILES) > 0;
		boolean includeRoot = (arg & INCLUDES_ROOT) > 0;
		if (!(artifactRepo && (includeFiles)))
			return;

		IArtifactKey key = ArtifactKey.parse("binary,sdk.rootfiles.win32.win32.x86,3.4.0.i0305"); //$NON-NLS-1$
		assertTrue(artifactRepository.contains(key));
		// File [] repoFiles = getRepoFiles();
		Map<String, Object[]> fileList = getRepoFiles(new HashMap<>());
		ZipInputStream zis = ((TestArtifactRepository) artifactRepository).getZipInputStream(key);

		TestData.assertContains(fileList, zis, !(!includeRoot && includeFiles && artifactRepo));
	}

	/**
	 *
	 * @return a list of relative files to the rootPath.
	 */
	private Map<String, Object[]> getRepoFiles(Map<String, Object[]> map) {
		if ((testArg & INCLUDES_FILES) > 0) {
			map = addEntry(map, "simpleconfigurator.source.jar"); //$NON-NLS-1$
			map = addEntry(map, FILE1);
			map = addEntry(map, FILE2);
		}
		return map;
	}

	@Override
	public void cleanup() {
		super.cleanup();

		if (artifactRepository != null)
			artifactRepository.removeAll(new NullProgressMonitor());
		artifactRepository = null;

		excludedFiles = null;

		if (adviceCollection != null)
			adviceCollection.clear();

		if (includedFiles != null)
			includedFiles = null;

		adviceCollection = null;
	}

	protected String toArgString(int arg) {
		String result = ""; //$NON-NLS-1$
		if ((arg & INCLUDES_ROOT) > 0)
			result += " INCLUDES_ROOT"; //$NON-NLS-1$
		if ((arg & EXCLUDES_UNUSED) > 0)
			result += " EXCLUDES_UNUSED"; //$NON-NLS-1$
		if ((arg & ARTIFACT_REPO) > 0)
			result += " ARTIFACT_REPO"; //$NON-NLS-1$
		if ((arg & INCLUDES_FILES) > 0)
			result += " INCLUDES_FILES"; //$NON-NLS-1$
		if ((arg & EXCLUDE_INCLUDED) > 0)
			result += " EXCLUDE_INCLUDED"; //$NON-NLS-1$
		return result;
	}

	private Map<String, Object[]> addEntry(Map<String, Object[]> map, String fileEntry) {
		try {
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			File contentBytes = new File(root, fileEntry);
			FileUtils.copyStream(new FileInputStream(contentBytes), false, content, true);
			boolean includeRootInEntry = ((testArg & INCLUDES_ROOT) > 0);
			String entry = includeRootInEntry ? new File(fileEntry).getPath() : new File(fileEntry).getName();
			entry = IPath.fromOSString(entry).toString();
			map.put(entry, new Object[] {contentBytes, content.toByteArray()});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
}
