/*******************************************************************************
 *  Copyright (c) 2014 SAP AG and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.*;

import java.util.*;
import junit.framework.TestCase;
import org.easymock.IAnswer;
import org.eclipse.equinox.internal.p2.director.app.IUListFormatter;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;

public class IUListFormatterTest extends TestCase {

	public void testFormat() {
		IUListFormatter format = new IUListFormatter("${id}=${id},${version},${org.eclipse.equinox.p2.name}");
		String result = format.format(asList(createIU("iu1", "1.0.0", "name", null)));
		assertEquals("iu1=iu1,1.0.0,name", result);
	}

	public void testFormat_MultipleIUs() {
		IUListFormatter format = new IUListFormatter("${id}=${version},${org.eclipse.equinox.p2.name}");
		String result = format.format(asList(//
				createIU("iu1", "1.0.0", "name", null),//
				createIU("iu2", "2.0.0", "name2", null)//
				));
		assertEquals("iu1=1.0.0,name" + System.getProperty("line.separator") + "iu2=2.0.0,name2", result);
	}

	public void testFormat_UnknownFormatOption() {
		IUListFormatter format = new IUListFormatter("${id}${UNKNOWN}");
		String result = format.format(asList(createIU("iu1", "1.0.0", "name", null)));
		assertEquals("iu1", result);
	}

	public void testFormat_Malformed() {
		IUListFormatter format = new IUListFormatter("${id=${version");
		String result = format.format(asList(createIU("iu1", "1.0.0", "name", null)));
		assertEquals("Input must be preserved", "${id=${version", result);
	}

	public static void main(String[] args) {
		IUListFormatter format = new IUListFormatter("${id=${version");
		List<IInstallableUnit> ius = new ArrayList<IInstallableUnit>(20000);
		for (int i = 0; i < 20000; i++) {
			ius.add(createIU("iu_" + i, "1.0.0", 30));
		}
		long start = System.currentTimeMillis();
		format.format(ius);
		System.out.println(System.currentTimeMillis() - start);
	}

	private static IInstallableUnit createIU(String id, String version, String name, String description) {
		IInstallableUnit iu = createNiceMock(id, IInstallableUnit.class);
		expect(iu.getId()).andStubReturn(id);
		expect(iu.getVersion()).andStubReturn(Version.create(version));

		final Map<String, String> properties = new HashMap<String, String>(3, 1);
		properties.put(IInstallableUnit.PROP_NAME, name);
		properties.put(IInstallableUnit.PROP_DESCRIPTION, description);
		expect(iu.getProperties()).andStubReturn(properties);
		expect(iu.getProperty((String) anyObject())).andStubAnswer(new MapAnswer(properties));
		expect(iu.getProperty((String) anyObject(), (String) anyObject())).andStubAnswer(new MapAnswer(properties));
		replay(iu);
		return iu;
	}

	private static IInstallableUnit createIU(String id, String version, int propCount) {
		IInstallableUnit iu = createNiceMock(id, IInstallableUnit.class);
		expect(iu.getId()).andStubReturn(id);
		expect(iu.getVersion()).andStubReturn(Version.create(version));

		final Map<String, String> properties = new HashMap<String, String>(propCount, 1);
		for (int i = 0; i < propCount; i++) {
			properties.put("prop_" + i, "propValue_" + i);
		}
		expect(iu.getProperties()).andStubReturn(properties);
		expect(iu.getProperty((String) anyObject())).andStubAnswer(new MapAnswer(properties));
		expect(iu.getProperty((String) anyObject(), (String) anyObject())).andStubAnswer(new MapAnswer(properties));
		replay(iu);
		return iu;
	}

	private static final class MapAnswer<T> implements IAnswer<T> {
		private final Map<?, T> map;

		MapAnswer(Map<?, T> map) {
			this.map = map;
		}

		public T answer() throws Throwable {
			return map.get(getCurrentArguments()[0]);
		}
	}

}
