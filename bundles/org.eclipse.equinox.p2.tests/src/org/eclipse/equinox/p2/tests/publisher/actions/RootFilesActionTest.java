/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.expect;

import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;

@SuppressWarnings({"unchecked"})
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

	public void insertPublisherInfoBehavior() {
		expect(publisherInfo.getArtifactRepository()).andReturn(artifactRepository).anyTimes();
		expect(publisherInfo.getArtifactOptions()).andReturn(IPublisherInfo.A_INDEX | IPublisherInfo.A_OVERWRITE | IPublisherInfo.A_PUBLISH).anyTimes();
		expect(publisherInfo.getAdvice(configSpec, true, null, null, IRootFilesAdvice.class)).andReturn(adviceCollection).anyTimes();
		expect(publisherInfo.getAdvice(configSpec, false, flavorArg + topArg, versionArg, ITouchpointAdvice.class)).andReturn(null).anyTimes();
	}

	private void setupTestCase(int testArg) throws Exception {
		this.testArg = testArg;
		adviceCollection = new ArrayList();
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
		Map fileList = getRepoFiles(new HashMap());
		ZipInputStream zis = ((TestArtifactRepository) artifactRepository).getZipInputStream(key);

		TestData.assertContains(fileList, zis, !(!includeRoot && includeFiles && artifactRepo));
	}

	/**
	 * 
	 * @return a list of relative files to the rootPath.
	 */
	private Map getRepoFiles(Map map) {
		if ((testArg & INCLUDES_FILES) > 0) {
			map = addEntry(map, "simpleconfigurator.source.jar"); //$NON-NLS-1$
			map = addEntry(map, FILE1);
			map = addEntry(map, FILE2);
		}
		return map;
	}

	public void cleanup() {
		super.cleanup();

		if (artifactRepository != null)
			artifactRepository.removeAll();
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

	private Map addEntry(Map map, String fileEntry) {
		try {
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			File contentBytes = new File(root, fileEntry);
			FileUtils.copyStream(new FileInputStream(contentBytes), false, content, true);
			boolean includeRootInEntry = ((testArg & INCLUDES_ROOT) > 0);
			String entry = includeRootInEntry ? new File(fileEntry).getPath() : new File(fileEntry).getName();
			entry = new Path(entry).toString();
			map.put(entry, new Object[] {contentBytes, content.toByteArray()});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
}
