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
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.index.Index;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;

/**
 * An index that maps id to IArtifactKey
 */
public class KeyIndex extends Index<IArtifactKey> {
	// Memory conserving map. Stores either String -> IArtifactKey
	// or String -> IArtifactKey[]
	// A stored array is always length >= 2.
	private final Map<String, Object> artifactMap;

	public KeyIndex(Collection<IArtifactKey> artifactKeys) {
		artifactMap = new HashMap<String, Object>(artifactKeys.size());
		for (IArtifactKey ak : artifactKeys) {
			Object prev = artifactMap.put(ak.getId(), ak);
			if (prev != null) {
				if (prev instanceof IArtifactKey)
					artifactMap.put(ak.getId(), new IArtifactKey[] {(IArtifactKey) prev, ak});
				else {
					IArtifactKey[] prevArr = (IArtifactKey[]) prev;
					IArtifactKey[] nxtArr = new IArtifactKey[prevArr.length + 1];
					System.arraycopy(prevArr, 0, nxtArr, 0, prevArr.length);
					nxtArr[prevArr.length] = ak;
					artifactMap.put(ak.getId(), nxtArr);
				}
			}
		}
	}

	public Iterator<IArtifactKey> getCandidates(IEvaluationContext ctx, IExpression variable, IExpression booleanExpr) {
		Object queriedKeys = getQueriedIDs(ctx, variable, ArtifactKey.MEMBER_ID, booleanExpr, null);
		if (queriedKeys == null)
			return null;

		Collection<IArtifactKey> collector = null;
		if (queriedKeys.getClass().isArray()) {
			Object[] keyArr = (Object[]) queriedKeys;
			int idx = keyArr.length;
			while (--idx >= 0) {
				Object v = artifactMap.get(keyArr[idx]);
				if (v == null)
					continue;
				if (collector == null)
					collector = new ArrayList<IArtifactKey>();
				if (v instanceof IArtifactKey)
					collector.add((IArtifactKey) v);
				else {
					IArtifactKey[] akArr = (IArtifactKey[]) v;
					for (IArtifactKey ak : akArr)
						collector.add(ak);
				}
			}
			if (collector == null)
				collector = Collections.<IArtifactKey> emptySet();
		} else {
			Object v = artifactMap.get(queriedKeys);
			if (v == null)
				collector = Collections.<IArtifactKey> emptySet();
			else if (v instanceof IArtifactKey)
				collector = Collections.singleton((IArtifactKey) v);
			else
				collector = CollectionUtils.unmodifiableList((IArtifactKey[]) v);
		}
		return collector.iterator();
	}
}
