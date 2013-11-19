/*******************************************************************************
 * Copyright (c) 2011 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.p2.metadata.expression.IMemberProvider;
import org.osgi.framework.ServiceReference;

public abstract class MemberProvider implements IMemberProvider {

	static class DictionaryMemberProvider extends MemberProvider {
		private final Dictionary<String, ? extends Object> dictionary;

		public DictionaryMemberProvider(Dictionary<String, ? extends Object> dictionary) {
			this.dictionary = dictionary;
		}

		public Object getMember(String memberName) {
			return dictionary.get(memberName);
		}
	}

	static class CIDictionaryMemberProvider extends DictionaryMemberProvider {
		public CIDictionaryMemberProvider(Dictionary<String, ? extends Object> dictionary) {
			super(lowerCaseKeys(dictionary));
		}

		@Override
		public Object getMember(String memberName) {
			return super.getMember(memberName == null ? null : memberName.toLowerCase());
		}

		private static Dictionary<String, ? extends Object> lowerCaseKeys(Dictionary<String, ? extends Object> dictionary) {
			boolean hasUpperCase = false;
			for (Enumeration<String> keys = dictionary.keys(); keys.hasMoreElements();) {
				String key = keys.nextElement();
				if (key.toLowerCase() != key) {
					hasUpperCase = true;
					break;
				}
			}
			if (!hasUpperCase)
				return dictionary;

			Dictionary<String, Object> lcMap = new Hashtable<String, Object>(dictionary.size());
			for (Enumeration<String> keys = dictionary.keys(); keys.hasMoreElements();) {
				String key = keys.nextElement();
				if (lcMap.put(key.toLowerCase(), dictionary.get(key)) != null)
					throw new IllegalArgumentException("case variants of the same key name: '" + key + '\''); //$NON-NLS-1$
			}
			return lcMap;
		}
	}

	static class MapMemberProvider extends MemberProvider {
		private final Map<String, ? extends Object> map;

		public MapMemberProvider(Map<String, ? extends Object> map) {
			this.map = map;
		}

		public Object getMember(String memberName) {
			return map.get(memberName);
		}
	}

	static class CIMapMemberProvider extends MapMemberProvider {
		public CIMapMemberProvider(Map<String, ? extends Object> map) {
			super(lowerCaseKeys(map));
		}

		@Override
		public Object getMember(String memberName) {
			return super.getMember(memberName == null ? null : memberName.toLowerCase());
		}

		private static Map<String, ? extends Object> lowerCaseKeys(Map<String, ? extends Object> map) {
			boolean hasUpperCase = false;
			Set<? extends Entry<String, ? extends Object>> entrySet = map.entrySet();
			for (Entry<String, ?> entry : entrySet) {
				String key = entry.getKey();
				String lowKey = key.toLowerCase();
				if (key != lowKey) {
					hasUpperCase = true;
					break;
				}
			}
			if (!hasUpperCase)
				return map;

			Map<String, Object> lcMap = new HashMap<String, Object>(map.size());
			for (Entry<String, ?> entry : entrySet) {
				if (lcMap.put(entry.getKey().toLowerCase(), entry.getValue()) != null)
					throw new IllegalArgumentException("case variants of the same key name: '" + entry.getKey() + '\''); //$NON-NLS-1$
			}
			return lcMap;
		}
	}

	static class ServiceRefMemberProvider extends MemberProvider {
		private final ServiceReference<?> serviceRef;

		public ServiceRefMemberProvider(ServiceReference<?> serviceRef) {
			this.serviceRef = serviceRef;
		}

		public Object getMember(String memberName) {
			return serviceRef.getProperty(memberName);
		}
	}

	private static final MemberProvider emptyProvider = create(Collections.emptyMap(), false);

	/**
	 * Create a new member provider on the given value. The value can be an instance of a {@link Map}, {@link Dictionary},
	 * or {@link ServiceReference}.
	 * @param value The value that provides the members
	 * @param caseInsensitive <code>true</code> if the members should be retrievable in a case insensitive way.
	 * @return A member provided that is backed by <code>value</code>. 
	 */
	@SuppressWarnings("unchecked")
	public static MemberProvider create(Object value, boolean caseInsensitive) {
		if (value instanceof Map<?, ?>)
			return caseInsensitive ? new CIMapMemberProvider((Map<String, ?>) value) : new MapMemberProvider((Map<String, ?>) value);
		if (value instanceof Dictionary<?, ?>)
			return caseInsensitive ? new CIDictionaryMemberProvider((Dictionary<String, ?>) value) : new DictionaryMemberProvider((Dictionary<String, ?>) value);
		if (value instanceof ServiceReference)
			return new ServiceRefMemberProvider((ServiceReference<?>) value);
		throw new IllegalArgumentException();
	}

	public static MemberProvider emptyProvider() {
		return emptyProvider;
	}
}
