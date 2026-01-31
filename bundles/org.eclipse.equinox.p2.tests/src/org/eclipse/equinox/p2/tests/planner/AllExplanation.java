/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
		ExplanationDeepConflict.class, ExplanationForOptionalDependencies.class,
		ExplanationForPartialInstallation.class, ExplanationLargeConflict.class,
		ExplanationSeveralConflictingRoots.class, MissingDependency.class, MissingNonGreedyRequirement.class,
		MissingNonGreedyRequirement2.class, MultipleSingleton.class, PatchTest10.class, PatchTest12.class
})
public class AllExplanation {
// test suite
}
