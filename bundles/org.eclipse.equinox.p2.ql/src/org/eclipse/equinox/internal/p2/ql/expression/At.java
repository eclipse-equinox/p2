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
package org.eclipse.equinox.internal.p2.ql.expression;

import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.expression.Expression;
import org.eclipse.equinox.internal.p2.metadata.expression.Member;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.ql.IQLExpression;

/**
 * This class represents indexed or keyed access to an indexed collection
 * or a map.
 */
final class At extends org.eclipse.equinox.internal.p2.metadata.expression.At implements IQLExpression {
	At(Expression lhs, Expression rhs) {
		super(lhs, rhs);
	}

	protected Object handleMember(IEvaluationContext context, Member member, Object instance, boolean[] handled) {
		if (instance instanceof IInstallableUnit) {
			if (InstallableUnit.MEMBER_TRANSLATED_PROPERTIES == member.getName() || InstallableUnit.MEMBER_PROFILE_PROPERTIES == member.getName()) {
				IIndexProvider<?> indexProvider = context.getIndexProvider();
				if (indexProvider == null)
					throw new UnsupportedOperationException("No managed properties available to QL"); //$NON-NLS-1$
				handled[0] = true;
				return indexProvider.getManagedProperty(instance, member.getName(), rhs.evaluate(context));
			}
		}
		return super.handleMember(context, member, instance, handled);
	}
}
