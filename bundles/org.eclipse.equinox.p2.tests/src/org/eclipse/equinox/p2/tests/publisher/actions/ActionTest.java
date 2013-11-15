/*******************************************************************************
 * Copyright (c) 2008, 2012 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertThat;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.hamcrest.*;
import org.junit.Assert;

@SuppressWarnings({"cast", "unchecked"})
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
		List<String> result = new ArrayList<String>();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				result.add(token);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	protected void verifyProvidedCapability(Collection<IProvidedCapability> prov, String namespace, String name, Version version) {
		for (IProvidedCapability pc : prov) {
			if (pc.getName().equalsIgnoreCase(name) && pc.getNamespace().equalsIgnoreCase(namespace) && pc.getVersion().equals(version))
				return; // pass
		}
		Assert.fail("Missing ProvidedCapability: " + name + version.toString()); //$NON-NLS-1$
	}

	protected void verifyRequiredCapability(Collection<IRequirement> requirement, String namespace, String name, VersionRange range) {
		verifyRequiredCapability(requirement, namespace, name, range, 1, 1, true);
	}

	protected void verifyRequiredCapability(Collection<IRequirement> requirement, String namespace, String name, VersionRange range, int min, int max, boolean greedy) {
		for (Iterator iterator = requirement.iterator(); iterator.hasNext();) {
			IRequiredCapability required = (IRequiredCapability) iterator.next();
			if (required.getName().equalsIgnoreCase(name) && required.getNamespace().equalsIgnoreCase(namespace) && required.getRange().equals(range)) {
				String requirementDescr = "RequiredCapability " + name + " " + range.toString();
				Assert.assertEquals("Min of " + requirementDescr, min, required.getMin());
				Assert.assertEquals("Max of " + requirementDescr, max, required.getMax());
				Assert.assertEquals("Greedy of " + requirementDescr, greedy, required.isGreedy());
				return;
			}
		}
		Assert.fail("Missing RequiredCapability: " + name + " " + range.toString()); //$NON-NLS-1$
	}

	protected IInstallableUnit mockIU(String id, Version version) {
		IInstallableUnit result = createMock(IInstallableUnit.class);
		expect(result.getId()).andReturn(id).anyTimes();
		if (version == null)
			version = Version.emptyVersion;
		expect(result.getVersion()).andReturn(version).anyTimes();
		expect(result.getFilter()).andReturn(null).anyTimes();
		replay(result);
		return result;
	}

	protected Map getFileMap(Map map, File[] files, Path root) {
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory())
				map = getFileMap(map, files[i].listFiles(), root);
			else {
				if (files[i].getPath().endsWith(JAR))
					continue;
				try {
					ByteArrayOutputStream content = new ByteArrayOutputStream();
					File contentBytes = files[i];
					FileUtils.copyStream(new FileInputStream(contentBytes), false, content, true);

					IPath entryPath = new Path(files[i].getAbsolutePath());
					entryPath = entryPath.removeFirstSegments(root.matchingFirstSegments(entryPath));
					entryPath = entryPath.setDevice(null);
					map.put(entryPath.toString(), new Object[] {contentBytes, content.toByteArray()});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	protected void contains(Collection<IProvidedCapability> capabilities, String namespace, String name, Version version) {
		for (IProvidedCapability capability : capabilities) {
			if (capability.getNamespace().equals(namespace) && capability.getName().equals(name) && capability.getVersion().equals(version))
				return;
		}
		fail();
	}

	protected void contains(Collection<IRequirement> capabilities, String namespace, String name, VersionRange range, String filterStr, boolean optional, boolean multiple) {
		IMatchExpression<IInstallableUnit> filter = InstallableUnit.parseFilter(filterStr);
		for (Iterator iterator = capabilities.iterator(); iterator.hasNext();) {
			IRequiredCapability capability = (IRequiredCapability) iterator.next();
			if (filter == null) {
				if (capability.getFilter() != null)
					continue;
			} else if (!filter.equals(capability.getFilter()))
				continue;
			if (!name.equals(capability.getName()))
				continue;
			if (!namespace.equals(capability.getNamespace()))
				continue;
			if (optional != (capability.getMin() == 0))
				continue;
			if (!range.equals(capability.getRange()))
				continue;
			return;
		}
		fail();
	}

	public void setupPublisherResult() {
		publisherResult = new PublisherResult();
	}

	/**
	 * Call this method to setup Publisher Info, not <code>insertPublisherInfoBehavior</code>
	 */
	public void setupPublisherInfo() {
		publisherInfo = createPublisherInfoMock();

		String[] config = getArrayFromString(configSpec, COMMA_SEPARATOR);
		expect(publisherInfo.getConfigurations()).andReturn(config).anyTimes();
		insertPublisherInfoBehavior();
		replay(publisherInfo);
	}

	/**
	 * Creates the mock object for the IPublisherInfo. Subclasses
	 * can override to create a nice or strict mock instead.
	 * @return The publisher info mock
	 * @see org.easymock.EasyMock#createNiceMock(Class)
	 * @see org.easymock.EasyMock#createStrictMock(Class)
	 */
	protected IPublisherInfo createPublisherInfoMock() {
		return createMock(IPublisherInfo.class);
	}

	/**
	 * Do not call this method, it is called by <code>setupPublisherInfo</code>.
	 */
	protected void insertPublisherInfoBehavior() {
		expect(publisherInfo.getMetadataRepository()).andReturn(createTestMetdataRepository(new IInstallableUnit[0])).anyTimes();
		expect(publisherInfo.getContextMetadataRepository()).andReturn(createTestMetdataRepository(new IInstallableUnit[0])).anyTimes();
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
		return new TypeSafeMatcher<IPublisherResult>() {

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
		return new TypeSafeMatcher<IPublisherResult>() {

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
