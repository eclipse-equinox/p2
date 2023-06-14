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
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

public abstract class ActionTest extends AbstractProvisioningTest {
	protected static final String COMMA_SEPARATOR = ","; //$NON-NLS-1$
	protected static final String JAR = "jar";//$NON-NLS-1$
	private static final boolean DEBUG = false;

	protected String os = "win32";//$NON-NLS-1$
	protected String ws = "win32";//$NON-NLS-1$
	protected String arch = "x86";//$NON-NLS-1$
	protected String configSpec = AbstractPublisherAction.createConfigSpec(ws, os, arch);//"win32.win32.x86"; // or macosx
	protected String flavorArg = "tooling";//$NON-NLS-1$
	protected String[] topLevel;
	protected AbstractPublisherAction testAction;
	protected IPublisherInfo publisherInfo;
	protected IPublisherResult publisherResult;

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 */
	public static String[] getArrayFromString(String list, String separator) {
		if (list == null || list.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		List<String> result = new ArrayList<>();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				result.add(token);
		}
		return result.toArray(new String[result.size()]);
	}

	protected void verifyProvidedCapability(Collection<IProvidedCapability> prov, String namespace, String name, Version version) {
		for (IProvidedCapability pc : prov) {
			if (pc.getName().equalsIgnoreCase(name) && pc.getNamespace().equalsIgnoreCase(namespace) && pc.getVersion().equals(version))
				return; // pass
		}
		Assert.fail("Missing ProvidedCapability: " + name + version.toString()); //$NON-NLS-1$
	}

	protected void verifyRequirement(Collection<IRequirement> actual, String namespace, String name, VersionRange range) {
		verifyRequirement(actual, namespace, name, range, null, 1, 1, true);
	}

	protected void verifyRequirement(Collection<IRequirement> actual, String namespace, String name, VersionRange range, String envFilter, int minCard, int maxCard, boolean greedy) {
		IRequirement expected = MetadataFactory.createRequirement(namespace, name, range, InstallableUnit.parseFilter(envFilter), minCard, maxCard, greedy);
		verifyRequirement(actual, expected);
	}

	protected void verifyRequirement(Collection<IRequirement> actual, String namespace, String propsFilter, String envFilter, int minCard, int maxCard, boolean greedy) {
		IRequirement expected = MetadataFactory.createRequirement(namespace, propsFilter, InstallableUnit.parseFilter(envFilter), minCard, maxCard, greedy);
		verifyRequirement(actual, expected);
	}

	/**
	 * Safe to use only if actual and expected were created by the same method of {@link MetadataFactory}
	 * because match expressions are not safe to compare for equality.
	 *
	 * This must be guaranteed by all sub-class test cases
	 *
	 * @param actual
	 * @param expected
	 */
	protected void verifyRequirement(Collection<IRequirement> actual, IRequirement expected) {
		for (IRequirement act : actual) {
			if (expected.getMatches().equals(act.getMatches())) {
				String descr = "IRequirement " + expected.getMatches();
				assertEquals("Min of " + descr, expected.getMin(), act.getMin());
				assertEquals("Max of " + descr, expected.getMax(), act.getMax());
				assertEquals("Greedy of " + descr, expected.isGreedy(), act.isGreedy());
				return;
			}
		}
		Assert.fail("Missing IRequirement: " + expected); //$NON-NLS-1$
	}

	protected IInstallableUnit mockIU(String id, Version version) {
		IInstallableUnit result = mock(IInstallableUnit.class);
		when(result.getId()).thenReturn(id);
		if (version == null)
			version = Version.emptyVersion;
		when(result.getVersion()).thenReturn(version);
		when(result.getFilter()).thenReturn(null);
		return result;
	}

	protected Map<String, Object[]> getFileMap(Map<String, Object[]> map, File[] files, IPath root) {
		for (File file : files) {
			if (file.isDirectory()) {
				map = getFileMap(map, file.listFiles(), root);
			} else {
				if (file.getPath().endsWith(JAR)) {
					continue;
				}
				try {
					ByteArrayOutputStream content = new ByteArrayOutputStream();
					File contentBytes = file;
					FileUtils.copyStream(new FileInputStream(contentBytes), false, content, true);
					IPath entryPath = IPath.fromOSString(file.getAbsolutePath());
					entryPath = entryPath.removeFirstSegments(root.matchingFirstSegments(entryPath));
					entryPath = entryPath.setDevice(null);
					map.put(entryPath.toString(), new Object[] {contentBytes, content.toByteArray()});
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	public void setupPublisherResult() {
		publisherResult = new PublisherResult();
	}

	/**
	 * Call this method to setup Publisher Info, not <code>insertPublisherInfoBehavior</code>
	 */
	public void setupPublisherInfo() {
		publisherInfo = mock(IPublisherInfo.class);

		String[] config = getArrayFromString(configSpec, COMMA_SEPARATOR);
		when(publisherInfo.getConfigurations()).thenReturn(config);
		insertPublisherInfoBehavior();
	}

	/**
	 * Do not call this method, it is called by <code>setupPublisherInfo</code>.
	 */
	protected void insertPublisherInfoBehavior() {
		when(publisherInfo.getMetadataRepository()).thenReturn(createTestMetdataRepository(new IInstallableUnit[0]));
		when(publisherInfo.getContextMetadataRepository())
				.thenReturn(createTestMetdataRepository(new IInstallableUnit[0]));
	}

	public void cleanup() {
		publisherInfo = null;
		publisherResult = null;
	}

	/**
	 * Prints a message used for debugging tests.
	 */
	public void debug(String message) {
		if (DEBUG)
			debug(message);
	}

	/**
	 * Adds an installable unit in the context visible to actions.
	 *
	 * @see AbstractPublisherAction#queryForIU
	 */
	protected final void addContextIU(String unitId, String unitVersion) {
		// could also be added to a context metadata repository in the publisher info, but this is easier
		publisherResult.addIU(createIU(unitId, Version.create(unitVersion)), IPublisherResult.NON_ROOT);
	}

	/**
	 * Adds an installable unit in the context visible to actions.
	 *
	 * @see AbstractPublisherAction#queryForIU
	 */
	protected final void addContextIU(String unitId, String unitVersion, String filter) {
		publisherResult.addIU(createIU(unitId, Version.create(unitVersion), filter, NO_PROVIDES), IPublisherResult.NON_ROOT);
	}

	/**
	 * Queries the publisher result for installable units with the given ID, and
	 * returns the result if there is exactly one such IU, or fails otherwise.
	 */
	protected final IInstallableUnit getUniquePublishedIU(String id) {
		assertThat(publisherResult, containsUniqueIU(id));

		IQueryResult<IInstallableUnit> queryResult = publisherResult.query(QueryUtil.createIUQuery(id), new NullProgressMonitor());
		return queryResult.iterator().next();
	}

	public static Matcher<IPublisherResult> containsIU(final String id) {
		final IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id);
		return new TypeSafeMatcher<>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("contains a unit " + id);
			}

			@Override
			public boolean matchesSafely(IPublisherResult item) {
				IQueryResult<IInstallableUnit> queryResult = item.query(query, null);
				return queryResultSize(queryResult) > 0;
			}
		};
	}

	public static Matcher<IPublisherResult> containsUniqueIU(final String id) {
		final IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id);
		return new TypeSafeMatcher<>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("contains exactly one unit " + id);
			}

			@Override
			public boolean matchesSafely(IPublisherResult item) {
				IQueryResult<IInstallableUnit> queryResult = item.query(query, null);
				return queryResultSize(queryResult) == 1;
			}
		};
	}

}
