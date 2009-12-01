/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ql;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public final class LocalizedMap extends Constructor {

	static final String KEYWORD = "localizedMap"; //$NON-NLS-1$

	private static final WeakHashMap localizedMapCache = new WeakHashMap();

	private static final ContextExpression localizedPropertiesExpr = new ExpressionParser().parseQuery("" + // //$NON-NLS-1$
			"everything.select(f | f ~= $2 &&" + // //$NON-NLS-1$
			"  f.host.exists(h | $0 ~= h) &&" + // //$NON-NLS-1$
			"  f.providedCapabilities.exists(pc | pc.namespace == 'org.eclipse.equinox.p2.localization' && pc.name ~= $1))" + // //$NON-NLS-1$
			".collect(f | f.properties).flatten()"); //$NON-NLS-1$

	public LocalizedMap(Expression[] operands) {
		super(operands);
		if (operands.length != 2)
			throw new IllegalArgumentException(KEYWORD + " must have exactly two arguments. The Locale and the IU"); //$NON-NLS-1$
	}

	public synchronized Object evaluate(ExpressionContext context, VariableScope scope) {
		Object arg = operands[0].evaluate(context, scope);
		if (!(arg instanceof Locale))
			throw new IllegalArgumentException(KEYWORD + " first argument must be a java.util.Locale"); //$NON-NLS-1$
		Locale locale = (Locale) arg;

		arg = operands[1].evaluate(context, scope);
		if (!(arg instanceof IInstallableUnit))
			throw new IllegalArgumentException(KEYWORD + " second argument must be an IInstallableUnit"); //$NON-NLS-1$

		IInstallableUnit iu = (IInstallableUnit) arg;
		if (iu instanceof IInstallableUnitFragment) {
			// Check that this isn't a translation fragment in itself.
			IProvidedCapability[] provides = iu.getProvidedCapabilities();
			int idx = provides.length;
			while (--idx >= 0) {
				IProvidedCapability pc = provides[idx];
				if (pc.getNamespace().equals("org.eclipse.equinox.p2.localization")) //$NON-NLS-1$
					return Collections.EMPTY_MAP;
			}
		}

		Map iuLocales;
		synchronized (localizedMapCache) {
			iuLocales = (Map) localizedMapCache.get(iu);
			if (iuLocales == null) {
				iuLocales = new HashMap();
				localizedMapCache.put(iu, iuLocales);
			}
		}

		Map properties;
		synchronized (iuLocales) {
			properties = (Map) iuLocales.get(locale);
			if (properties == null) {
				properties = new HashMap();
				collectLocaleProperties(context, scope, locale, iu, properties);
				iuLocales.put(locale, properties);
			}
		}
		return properties;
	}

	int countReferenceToEverything() {
		return 1;
	}

	private static void collectLocaleProperties(ExpressionContext context, VariableScope scope, Locale locale, IInstallableUnit iu, Map properties) {
		List localePrefixes = getLocalePrefixes(locale);
		addPrefixedProperties(localePrefixes, properties, iu.getProperties().entrySet().iterator());
		ExpressionContext subContext = new ExpressionContext(IInstallableUnitFragment.class, new Object[] {iu, locale, IInstallableUnitFragment.class}, context.getCopy(), false);
		addPrefixedProperties(localePrefixes, properties, localizedPropertiesExpr.evaluateAsIterator(subContext, new VariableScope()));
	}

	private static void addPrefixedProperties(List prefixes, Map properties, Iterator entries) {
		int prefixCount = prefixes.size();
		outer: while (entries.hasNext()) {
			Map.Entry entry = (Map.Entry) entries.next();
			String key = (String) entry.getKey();
			for (int idx = 0; idx < prefixCount; ++idx) {
				if (key.startsWith((String) prefixes.get(idx))) {
					properties.put(key, entry.getValue());
					continue outer;
				}
			}
		}
	}

	private static List getLocalePrefixes(Locale locale) {
		ArrayList keyList = new ArrayList();

		StringBuffer bld = new StringBuffer();
		bld.append(locale.getLanguage());
		int pos = bld.length();
		bld.append('.');
		keyList.add(bld.toString());
		bld.setLength(pos);

		bld.append('_');
		if (locale.getCountry().length() > 0) {
			bld.append(locale.getCountry());
			pos = bld.length();
			bld.append('.');
			keyList.add(bld.toString());
			bld.setLength(pos);
		}

		if (locale.getVariant().length() > 0) {
			bld.append('_');
			bld.append(locale.getVariant());
			bld.append('.');
			keyList.add(bld.toString());
		}
		keyList.add("df_LT."); //$NON-NLS-1$
		return keyList;
	}

	String getOperator() {
		return KEYWORD;
	}
}
