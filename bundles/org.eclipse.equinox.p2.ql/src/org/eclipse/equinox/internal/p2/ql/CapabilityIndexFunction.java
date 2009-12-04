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
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;

/**
 * A function that creates a {@link CapabilityIndex} based on a collection of
 * {@link IInstallableUnit} instances.
 */
public final class CapabilityIndexFunction extends Function {

	static final String KEYWORD = "capabilityIndex"; //$NON-NLS-1$

	static class IUCapability {
		final IInstallableUnit iu;
		final IProvidedCapability capability;

		public IUCapability(IInstallableUnit iu, IProvidedCapability capability) {
			this.iu = iu;
			this.capability = capability;
		}
	}

	public CapabilityIndexFunction(Expression[] operands) {
		super(operands);
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		HashMap index = new HashMap();
		Iterator itor = operands[0].evaluateAsIterator(context, scope);
		while (itor.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) itor.next();
			IProvidedCapability[] pcs = iu.getProvidedCapabilities();
			int idx = pcs.length;
			while (--idx >= 0) {
				IProvidedCapability pc = pcs[idx];
				IUCapability iuCap = new IUCapability(iu, pc);
				String name = pc.getName();
				Object prev = index.put(name, iuCap);
				if (prev != null) {
					ArrayList lst;
					if (prev instanceof ArrayList)
						lst = (ArrayList) prev;
					else {
						lst = new ArrayList(4);
						lst.add(prev);
					}
					lst.add(iuCap);
					index.put(name, lst);
				}
			}
		}
		return new CapabilityIndex(index);
	}

	String getOperator() {
		return KEYWORD;
	}
}
