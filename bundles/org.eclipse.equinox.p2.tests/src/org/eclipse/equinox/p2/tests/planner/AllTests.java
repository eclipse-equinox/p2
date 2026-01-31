/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Performs all automated planner tests.
 */
@Suite
@SelectClasses({ AbsolutePlanTest.class, ActualChangeRequestTest.class, ActualChangeRequestTest2.class,
		AdditionalConstraints.class, AddIUProperty.class, AgentPlanTestInRunningInstance.class,
		AgentPlanTestInExternalInstance.class, AgentPlanTestInExternalInstanceForCohostedMode.class, AllOptional.class,
		AllOrbit.class, AllRequestFlexerTests.class, AnotherSingleton.class, Bug207319.class, Bug249605.class,
		Bug252638.class, Bug254481dataSet1.class, Bug254481dataSet2.class, Bug255984.class, Bug259537.class,
		Bug262580.class, Bug270656.class, Bug270668.class, Bug270683.class, Bug271067.class, Bug271954.class,
		Bug278668.class, Bug300572.class, Bug300572Small2.class, Bug300572Small3.class, Bug300572Small4.class,
		Bug300572Small5.class, Bug300572Small6.class, Bug302582.class, Bug302582b.class, Bug302582c.class,
		Bug306424.class, Bug306279c.class, Bug306279d.class, Bug311330.class, Bug329279.class, DependencyOnSelf.class,
		DisabledExplanation.class, DropinsScenario.class, EPPPackageInstallStability_bug323322.class,
		ExplanationDeepConflict.class, ExplanationForOptionalDependencies.class,
		ExplanationForPartialInstallation.class, ExplanationLargeConflict.class,
		ExplanationSeveralConflictingRoots.class, FindRootsAfterUpdate.class, FromStrictToOptional.class,
		GreedyRequirement.class, InclusionRuleTest.class, InclusionRuleTest2.class, IUProperties.class,
		IUPropertyRemoval.class, IUWithFilter.class, IUWithFilter2.class, MinimalInstall.class, MinimalInstall2.class,
		MissingDependency.class, MissingDependency2.class, MissingDependency3.class, MissingNonGreedyRequirement.class,
		MissingNonGreedyRequirement2.class, MissingOptional.class, MissingOptionalNonGreedyRequirement.class,
		MissingOptionalWithDependencies.class, MissingOptionalWithDependencies2.class, NonMinimalState.class,
		NonMinimalState2.class, NoUnecessaryIUProperty.class, MultipleProvider.class, MultipleSingleton.class,
		NoRequirements.class, ORTesting.class, PatchTest1.class, PatchTest10.class, PatchTest11.class,
		PatchTest12.class, PatchTest13.class, PatchTest1b.class, PatchTest1c.class, PatchTest2.class, PatchTest3.class,
		PatchTest4.class, PatchTest5.class, PatchTest6.class, PatchTest7.class, PatchTest7b.class, PatchTest8.class,
		PatchTest9.class, PatchTest10.class, PatchTest12.class, PatchTestMultiplePatch.class,
		PatchTestMultiplePatch2.class, PatchTestMultiplePatch3.class, PatchTestOptional.class, PatchTestOptional2.class,
		PatchTestOptional3.class, PatchTestUninstall.class, PatchTestUpdate.class, PatchTestUpdate2.class,
		PatchTestUpdate3.class, PatchTestUpdate4.class, PatchTestUpdate5.class, PatchTestUsingNegativeRequirement.class,
		PermissiveSlicerTest.class, PP2ShouldFailToInstall.class, ResolvedIUInPCR.class, SDKPatchingTest1.class,
		SDKPatchingTest2.class, SeveralOptionalDependencies.class, SeveralOptionalDependencies2.class,
		SeveralOptionalDependencies3.class, SeveralOptionalDependencies4.class, SeveralOptionalDependencies5.class,
		SimpleOptionalTest.class, SimpleOptionalTest2.class, SimpleOptionalTest3.class, SimpleOptionalTest4.class,
		SimpleOptionalTest5.class, SimpleSingleton.class, SimulatedSharedInstallTest.class,
		SingletonOptionallyInstalled.class, SingletonOptionallyInstalled2.class, SWTFragment.class,
		SynchronizeOperationTest.class, TestNoopChangeRequest.class, TestFilteringOnAbsentProperty.class,
		TopLevelFilterTest.class, TwoVersionsOfWSDL.class, TychoUsage.class, UninstallEverything.class,
		UpdateForTwoIUs.class, UpdateQueryTest.class, })
public class AllTests {
// Enable the following
// Bug252682.class,
// Bug272251.class,
// Bug302582d.class,
// Bug306279.class,
// Bug306279b.class,
// MissingOptionalWithDependencies3.class, Disabled, see bug 277161
// NegationTesting.class,
// PatchFailingToInstall.class,
// ProvisioningPlanQueryTest.class, disabled, see bug 313812
}