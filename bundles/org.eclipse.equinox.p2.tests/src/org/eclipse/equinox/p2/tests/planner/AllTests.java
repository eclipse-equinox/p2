/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import junit.framework.*;

/**
 * Performs all automated planner tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(AbsolutePlanTest.class);
		suite.addTestSuite(ActualChangeRequestTest.class);
		suite.addTestSuite(ActualChangeRequestTest2.class);
		suite.addTestSuite(AdditionalConstraints.class);
		suite.addTestSuite(AddIUProperty.class);
		suite.addTestSuite(AgentPlanTestInRunningInstance.class);
		suite.addTestSuite(AgentPlanTestInExternalInstance.class);
		suite.addTestSuite(AgentPlanTestInExternalInstanceForCohostedMode.class);
		suite.addTestSuite(AllOptional.class);
		suite.addTestSuite(AllOrbit.class);
		suite.addTest(AllRequestFlexerTests.suite());
		suite.addTestSuite(AnotherSingleton.class);
		suite.addTestSuite(Bug207319.class);
		suite.addTestSuite(Bug249605.class);
		suite.addTestSuite(Bug252638.class);
		//		suite.addTestSuite(Bug252682.class);
		suite.addTestSuite(Bug254481dataSet1.class);
		suite.addTestSuite(Bug254481dataSet2.class);
		suite.addTestSuite(Bug255984.class);
		suite.addTestSuite(Bug259537.class);
		suite.addTestSuite(Bug262580.class);
		suite.addTestSuite(Bug270656.class);
		suite.addTestSuite(Bug270668.class);
		suite.addTestSuite(Bug270683.class);
		suite.addTestSuite(Bug271067.class);
		suite.addTestSuite(Bug271954.class);
		//		suite.addTestSuite(Bug272251.class);
		suite.addTestSuite(Bug278668.class);
		suite.addTestSuite(Bug300572.class);
		suite.addTestSuite(Bug300572Small2.class);
		suite.addTestSuite(Bug300572Small3.class);
		suite.addTestSuite(Bug300572Small4.class);
		suite.addTestSuite(Bug300572Small5.class);
		suite.addTestSuite(Bug300572Small6.class);
		suite.addTestSuite(Bug302582.class);
		suite.addTestSuite(Bug302582b.class);
		suite.addTestSuite(Bug302582c.class);
		//		suite.addTestSuite(Bug302582d.class);
		suite.addTestSuite(Bug306424.class);
		// suite.addTestSuite(Bug306279.class);
		// suite.addTestSuite(Bug306279b.class);
		suite.addTestSuite(Bug306279c.class);
		suite.addTestSuite(Bug306279d.class);
		suite.addTestSuite(Bug311330.class);
		suite.addTestSuite(Bug329279.class);
		suite.addTestSuite(DependencyOnSelf.class);
		suite.addTestSuite(DisabledExplanation.class);
		suite.addTestSuite(DropinsScenario.class);
		suite.addTestSuite(EPPPackageInstallStability_bug323322.class);
		suite.addTestSuite(ExplanationDeepConflict.class);
		suite.addTestSuite(ExplanationForOptionalDependencies.class);
		suite.addTestSuite(ExplanationForPartialInstallation.class);
		suite.addTestSuite(ExplanationLargeConflict.class);
		suite.addTestSuite(ExplanationSeveralConflictingRoots.class);
		suite.addTestSuite(FindRootsAfterUpdate.class);
		suite.addTestSuite(FromStrictToOptional.class);
		suite.addTestSuite(GreedyRequirement.class);
		suite.addTestSuite(InclusionRuleTest.class);
		suite.addTestSuite(InclusionRuleTest2.class);
		suite.addTestSuite(IUProperties.class);
		suite.addTestSuite(IUPropertyRemoval.class);
		suite.addTestSuite(IUWithFilter.class);
		suite.addTestSuite(IUWithFilter2.class);
		suite.addTestSuite(MinimalInstall.class);
		suite.addTestSuite(MinimalInstall2.class);
		suite.addTestSuite(MissingDependency.class);
		suite.addTestSuite(MissingDependency2.class);
		suite.addTestSuite(MissingDependency3.class);
		suite.addTestSuite(MissingNonGreedyRequirement.class);
		suite.addTestSuite(MissingNonGreedyRequirement2.class);
		suite.addTestSuite(MissingOptional.class);
		suite.addTestSuite(MissingOptionalNonGreedyRequirement.class);
		suite.addTestSuite(MissingOptionalWithDependencies.class);
		suite.addTestSuite(MissingOptionalWithDependencies2.class);
		//		suite.addTestSuite(MissingOptionalWithDependencies3.class);	Disabled, see bug 277161
		//		suite.addTestSuite(NegationTesting.class);
		suite.addTestSuite(NonMinimalState.class);
		suite.addTestSuite(NonMinimalState2.class);
		suite.addTestSuite(NoUnecessaryIUProperty.class);
		suite.addTestSuite(MultipleProvider.class);
		suite.addTestSuite(MultipleSingleton.class);
		suite.addTestSuite(NoRequirements.class);
		suite.addTestSuite(ORTesting.class);
		//		suite.addTestSuite(PatchFailingToInstall.class);
		suite.addTestSuite(PatchTest1.class);
		suite.addTestSuite(PatchTest10.class);
		suite.addTestSuite(PatchTest11.class);
		suite.addTestSuite(PatchTest12.class);
		suite.addTestSuite(PatchTest13.class);
		suite.addTestSuite(PatchTest1b.class);
		suite.addTestSuite(PatchTest1c.class);
		suite.addTestSuite(PatchTest2.class);
		suite.addTestSuite(PatchTest3.class);
		suite.addTestSuite(PatchTest4.class);
		suite.addTestSuite(PatchTest5.class);
		suite.addTestSuite(PatchTest6.class);
		suite.addTestSuite(PatchTest7.class);
		suite.addTestSuite(PatchTest7b.class);
		suite.addTestSuite(PatchTest8.class);
		suite.addTestSuite(PatchTest9.class);
		suite.addTestSuite(PatchTest10.class);
		suite.addTestSuite(PatchTest12.class);
		suite.addTestSuite(PatchTestMultiplePatch.class);
		suite.addTestSuite(PatchTestMultiplePatch2.class);
		suite.addTestSuite(PatchTestMultiplePatch3.class);
		suite.addTestSuite(PatchTestOptional.class);
		suite.addTestSuite(PatchTestOptional2.class);
		suite.addTestSuite(PatchTestOptional3.class);
		suite.addTestSuite(PatchTestUninstall.class);
		suite.addTestSuite(PatchTestUpdate.class);
		suite.addTestSuite(PatchTestUpdate2.class);
		suite.addTestSuite(PatchTestUpdate3.class);
		suite.addTestSuite(PatchTestUpdate4.class);
		suite.addTestSuite(PatchTestUpdate5.class);
		suite.addTestSuite(PatchTestUsingNegativeRequirement.class);
		suite.addTestSuite(PermissiveSlicerTest.class);
		suite.addTestSuite(PP2ShouldFailToInstall.class);
		suite.addTestSuite(ResolvedIUInPCR.class);
		//		suite.addTestSuite(ProvisioningPlanQueryTest.class); disabled, see bug 313812 
		suite.addTestSuite(SDKPatchingTest1.class);
		suite.addTestSuite(SDKPatchingTest2.class);
		suite.addTestSuite(SeveralOptionalDependencies.class);
		suite.addTestSuite(SeveralOptionalDependencies2.class);
		suite.addTestSuite(SeveralOptionalDependencies3.class);
		suite.addTestSuite(SeveralOptionalDependencies4.class);
		suite.addTestSuite(SeveralOptionalDependencies5.class);
		suite.addTestSuite(SimpleOptionalTest.class);
		suite.addTestSuite(SimpleOptionalTest2.class);
		suite.addTestSuite(SimpleOptionalTest3.class);
		suite.addTestSuite(SimpleOptionalTest4.class);
		suite.addTestSuite(SimpleOptionalTest5.class);
		suite.addTestSuite(SimpleSingleton.class);
		suite.addTestSuite(SimulatedSharedInstallTest.class);
		suite.addTestSuite(SingletonOptionallyInstalled.class);
		suite.addTestSuite(SingletonOptionallyInstalled2.class);
		suite.addTestSuite(SWTFragment.class);
		suite.addTestSuite(SynchronizeOperationTest.class);
		suite.addTestSuite(TestNoopChangeRequest.class);
		suite.addTestSuite(TestFilteringOnAbsentProperty.class);
		suite.addTestSuite(TopLevelFilterTest.class);
		suite.addTestSuite(TwoVersionsOfWSDL.class);
		suite.addTestSuite(TychoUsage.class);
		suite.addTestSuite(UninstallEverything.class);
		suite.addTestSuite(UpdateForTwoIUs.class);
		suite.addTestSuite(UpdateQueryTest.class);
		return suite;
	}
}