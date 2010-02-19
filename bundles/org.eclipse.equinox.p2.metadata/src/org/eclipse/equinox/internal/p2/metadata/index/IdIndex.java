/*******************************************************************************
 * Copyright (c) 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.index;

import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.IUMap;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;

public class IdIndex extends Index<IInstallableUnit> {
	private final IUMap iuMap;

	public IdIndex(IUMap iuMap) {
		this.iuMap = iuMap;
	}

	public IdIndex(Iterator<IInstallableUnit> ius) {
		iuMap = new IUMap();
		while (ius.hasNext())
			iuMap.add(ius.next());
	}

	public Iterator<IInstallableUnit> getCandidates(IEvaluationContext ctx, IExpression variable, IExpression booleanExpr) {
		Object queriedKeys = getQueriedIDs(ctx, variable, InstallableUnit.MEMBER_ID, booleanExpr, null);
		if (queriedKeys == null)
			return null;

		if (queriedKeys instanceof Collection<?>) {
			HashSet<IInstallableUnit> collector = new HashSet<IInstallableUnit>();
			for (Object key : (Collection<?>) queriedKeys)
				collector.addAll(iuMap.getUnits((String) key));
			return collector.iterator();
		}
		return iuMap.getUnits((String) queriedKeys).iterator();
	}
}
