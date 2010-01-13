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

import org.eclipse.equinox.internal.p2.metadata.expression.Expression;
import org.eclipse.equinox.internal.p2.metadata.expression.Member;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.ql.IQLExpression;
import org.eclipse.equinox.p2.ql.ITranslationSupport;

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
			if (IQLConstants.VARIABLE_TRANSLATIONS.equals(member.getName())) {
				ITranslationSupport ts = (ITranslationSupport) QLFactory.TRANSLATIONS.evaluate(context);
				handled[0] = true;
				return ts.getIUProperty((IInstallableUnit) instance, (String) rhs.evaluate(context));
			}
		}
		return super.handleMember(context, member, instance, handled);
	}
}
