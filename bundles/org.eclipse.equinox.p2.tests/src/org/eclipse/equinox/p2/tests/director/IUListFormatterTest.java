/*******************************************************************************
 *  Copyright (c) 2014, 2021 SAP AG and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.equinox.internal.p2.director.app.IUListFormatter;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class IUListFormatterTest {
	@Test
	public void testFormat() {
		IUListFormatter format = new IUListFormatter("${id}=${id},${version},${org.eclipse.equinox.p2.name}");
		String result = format.format(asList(createIU("iu1", "1.0.0", "name", null)));
		assertEquals("iu1=iu1,1.0.0,name", result);
	}

	@Test
	public void testFormat_MultipleIUs() {
		IUListFormatter format = new IUListFormatter("${id}=${version},${org.eclipse.equinox.p2.name}");
		String result = format.format(asList(//
				createIU("iu1", "1.0.0", "name", null), //
				createIU("iu2", "2.0.0", "name2", null)//
		));
		assertEquals("iu1=1.0.0,name" + System.lineSeparator() + "iu2=2.0.0,name2", result);
	}

	@Test
	public void testFormat_UnknownFormatOption() {
		IUListFormatter format = new IUListFormatter("${id}${UNKNOWN}");
		String result = format.format(asList(createIU("iu1", "1.0.0", "name", null)));
		assertEquals("iu1", result);
	}

	@Test
	public void testFormat_Malformed() {
		IUListFormatter format = new IUListFormatter("${id=${version");
		String result = format.format(asList(createIU("iu1", "1.0.0", "name", null)));
		assertEquals("Input must be preserved", "${id=${version", result);
	}

	public static void main(String[] args) {
		IUListFormatter format = new IUListFormatter("${id=${version");
		List<IInstallableUnit> ius = new ArrayList<>(20000);
		for (int i = 0; i < 20000; i++) {
			ius.add(createIU("iu_" + i, "1.0.0", 30));
		}
		long start = System.currentTimeMillis();
		format.format(ius);
		System.out.println(System.currentTimeMillis() - start);
	}

	private static IInstallableUnit createIU(String id, String version, String name, String description) {
		IInstallableUnit iu = mock(IInstallableUnit.class);
		when(iu.getId()).thenReturn(id);
		when(iu.getVersion()).thenReturn(Version.create(version));

		final Map<String, String> properties = new HashMap<>(3, 1);
		properties.put(IInstallableUnit.PROP_NAME, name);
		properties.put(IInstallableUnit.PROP_DESCRIPTION, description);
		when(iu.getProperties()).thenReturn(properties);
		when(iu.getProperty(anyString())).thenAnswer(new MapAnswer<>(properties));
		when(iu.getProperty(anyString(), anyString())).thenAnswer(new MapAnswer<>(properties));
		return iu;
	}

	private static IInstallableUnit createIU(String id, String version, int propCount) {
		IInstallableUnit iu = mock(IInstallableUnit.class);
		when(iu.getId()).thenReturn(id);
		when(iu.getVersion()).thenReturn(Version.create(version));

		final Map<String, String> properties = new HashMap<>(propCount, 1);
		for (int i = 0; i < propCount; i++) {
			properties.put("prop_" + i, "propValue_" + i);
		}
		when(iu.getProperties()).thenReturn(properties);
		when(iu.getProperty(anyString())).thenAnswer(new MapAnswer<>(properties));
		when(iu.getProperty(anyString(), anyString())).thenAnswer(new MapAnswer<>(properties));
		return iu;
	}

	private static final class MapAnswer<T> implements Answer<T> {
		private final Map<?, T> map;

		MapAnswer(Map<?, T> map) {
			this.map = map;
		}

		@Override
		public T answer(InvocationOnMock arg) throws Throwable {
			return map.get(arg.getArguments()[0]);
		}
	}

}
