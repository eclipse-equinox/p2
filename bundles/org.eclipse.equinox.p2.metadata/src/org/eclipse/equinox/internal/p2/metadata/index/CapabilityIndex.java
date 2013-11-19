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
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.internal.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.*;

/**
 * An in-memory implementation of a CapabilityIndex based on a Map.
 */
@SuppressWarnings("unchecked")
public class CapabilityIndex extends Index<IInstallableUnit> {

	private final Map<String, Object> capabilityMap;

	public CapabilityIndex(Iterator<IInstallableUnit> itor) {
		HashMap<String, Object> index = new HashMap<String, Object>(300);
		while (itor.hasNext()) {
			IInstallableUnit iu = itor.next();
			Collection<IProvidedCapability> pcs = iu.getProvidedCapabilities();
			for (IProvidedCapability pc : pcs) {
				String name = pc.getName();
				Object prev = index.put(name, iu);
				if (prev == null || prev == iu)
					continue;

				ArrayList<IInstallableUnit> list;
				if (prev instanceof IInstallableUnit) {
					list = new ArrayList<IInstallableUnit>();
					list.add((IInstallableUnit) prev);
				} else
					list = (ArrayList<IInstallableUnit>) prev;
				list.add(iu);
				index.put(name, list);
			}
		}
		this.capabilityMap = index;
	}

	private Object getRequirementIDs(IEvaluationContext ctx, IExpression requirement, Object queriedKeys) {
		switch (requirement.getExpressionType()) {
			case IExpression.TYPE_AND :
				// AND is OK if at least one of the branches require the queried key
				for (IExpression expr : ExpressionUtil.getOperands(requirement)) {
					Object test = getRequirementIDs(ctx, expr, queriedKeys);
					if (test != null) {
						if (test == Boolean.FALSE)
							// Failing exists so the AND will fail altogether
							return test;

						// It's safe to break here since an and'ing several queries
						// for different keys and the same input will yield false anyway.
						return test;
					}
				}
				return null;

			case IExpression.TYPE_OR :
				// OR is OK if all the branches require the queried key
				for (IExpression expr : ExpressionUtil.getOperands(requirement)) {
					Object test = getRequirementIDs(ctx, expr, queriedKeys);
					if (test == null)
						// This branch did not require the key so index cannot be used
						return null;

					if (test == Boolean.FALSE)
						// Branch will always fail regardless of input, so just ignore
						continue;

					queriedKeys = test;
				}
				return queriedKeys;

			case IExpression.TYPE_ALL :
			case IExpression.TYPE_EXISTS :
				CollectionFilter cf = (CollectionFilter) requirement;
				if (isIndexedMember(cf.getOperand(), ExpressionFactory.THIS, InstallableUnit.MEMBER_PROVIDED_CAPABILITIES)) {
					LambdaExpression lambda = cf.lambda;
					return getQueriedIDs(ctx, lambda.getItemVariable(), ProvidedCapability.MEMBER_NAME, lambda.getOperand(), queriedKeys);
				}
		}
		return null;
	}

	@Override
	protected Object getQueriedIDs(IEvaluationContext ctx, IExpression variable, String memberName, IExpression booleanExpr, Object queriedKeys) {
		if (booleanExpr.getExpressionType() != IExpression.TYPE_MATCHES)
			return super.getQueriedIDs(ctx, variable, memberName, booleanExpr, queriedKeys);

		Matches matches = (Matches) booleanExpr;
		if (matches.lhs != variable)
			return null;

		Object rhsObj = matches.rhs.evaluate(ctx);
		if (!(rhsObj instanceof IRequirement))
			return null;

		// Let the requirement expression participate in the
		// index usage query
		//
		IMatchExpression<IInstallableUnit> rm = ((IRequirement) rhsObj).getMatches();
		return RequiredCapability.isSimpleRequirement(rm) ? concatenateUnique(queriedKeys, rm.getParameters()[0]) : getRequirementIDs(rm.createContext(), ((Unary) rm).operand, queriedKeys);
	}

	public Iterator<IInstallableUnit> getCandidates(IEvaluationContext ctx, IExpression variable, IExpression booleanExpr) {
		Object queriedKeys = null;

		// booleanExpression must be a collection filter on providedCapabilities
		// or an IInstallableUnit used in a match expression.
		//
		IExpression expr = booleanExpr;
		int type = booleanExpr.getExpressionType();
		if (type == 0) {
			// wrapper
			expr = ((Unary) booleanExpr).operand;
			type = expr.getExpressionType();
		}

		switch (type) {
			case IExpression.TYPE_ALL :
			case IExpression.TYPE_EXISTS :
				CollectionFilter cf = (CollectionFilter) expr;

				if (isIndexedMember(cf.getOperand(), variable, InstallableUnit.MEMBER_PROVIDED_CAPABILITIES)) {
					// This is providedCapabilities.exists or providedCapabilites.all
					//
					LambdaExpression lambda = cf.lambda;
					queriedKeys = getQueriedIDs(ctx, lambda.getItemVariable(), ProvidedCapability.MEMBER_NAME, lambda.getOperand(), queriedKeys);
				} else {
					// Might be the requirements array.
					//
					Expression op = cf.getOperand();
					if (op instanceof Member && InstallableUnit.MEMBER_REQUIREMENTS.equals(((Member) op).getName())) {
						queriedKeys = getQueriedIDs(ctx, variable, ProvidedCapability.MEMBER_NAME, booleanExpr, queriedKeys);
					}
				}
				if (queriedKeys == null) {
					// Might be a parameterized query of requirements
					// If matching class is InstallableUnit && paramter exists && parameter is IRequirement
					if (cf.getOperand() instanceof Parameter && ctx.getParameter(0) instanceof Collection<?>) {
						// Check that the parameter really is the requirement array
						// This only really works for IRequiredCapabilities, not any IRequirements
						Collection<?> collection = (Collection<?>) ctx.getParameter(0);
						boolean instance = !collection.isEmpty();
						for (Object object : collection) {
							instance &= (object instanceof IRequiredCapability);
						}
						if (instance) {
							Collection<String> result = new ArrayList<String>();
							for (Object object : collection) {
								// This instance of check was done above
								IRequiredCapability capability = (IRequiredCapability) object;
								result.add(capability.getName());
							}
							if (result.size() > 0) {
								queriedKeys = result;
							}
						}
					}
				}
				break;

			case IExpression.TYPE_MATCHES :
				Matches matches = (Matches) expr;
				if (matches.lhs != variable)
					break;

				Object rhsObj = matches.rhs.evaluate(ctx);
				if (!(rhsObj instanceof IRequirement))
					break;

				// Let the requirement expression participate in the
				// index usage query
				//
				IMatchExpression<IInstallableUnit> rm = ((IRequirement) rhsObj).getMatches();
				queriedKeys = RequiredCapability.isSimpleRequirement(rm) ? concatenateUnique(queriedKeys, rm.getParameters()[0]) : getRequirementIDs(rm.createContext(), ((Unary) rm).operand, queriedKeys);
				break;

			default :
				queriedKeys = null;
		}

		if (queriedKeys == null)
			// Index cannot be used.
			return null;

		Collection<IInstallableUnit> matchingIUs;
		if (queriedKeys == Boolean.FALSE) {
			// It has been determined that the expression has no chance
			// to succeed regardless of input
			matchingIUs = Collections.<IInstallableUnit> emptySet();
		} else if (queriedKeys instanceof Collection<?>) {
			matchingIUs = new HashSet<IInstallableUnit>();
			for (Object key : (Collection<Object>) queriedKeys)
				collectMatchingIUs((String) key, matchingIUs);
		} else {
			Object v = capabilityMap.get(queriedKeys);
			if (v == null)
				matchingIUs = Collections.<IInstallableUnit> emptySet();
			else if (v instanceof IInstallableUnit)
				matchingIUs = Collections.singleton((IInstallableUnit) v);
			else
				matchingIUs = (Collection<IInstallableUnit>) v;
		}
		return matchingIUs.iterator();
	}

	private void collectMatchingIUs(String name, Collection<IInstallableUnit> collector) {
		Object v = capabilityMap.get(name);
		if (v == null)
			return;
		if (v instanceof IInstallableUnit)
			collector.add((IInstallableUnit) v);
		else
			collector.addAll((Collection<IInstallableUnit>) v);
	}
}
