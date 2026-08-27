/*******************************************************************************
 *  Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.ui.dialogs;

import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.model.HistoryDiffElement.ChangeType;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.equinox.p2.ui.RevertProfilePage;

/**
 * Tests for the two-snapshot diff classification logic in {@link RevertProfilePage}.
 */
public class RevertProfilePageDiffTest extends AbstractProvisioningUITest {

	private RevertProfilePage page;
	private IProfileRegistry registry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		page = new RevertProfilePage();
		page.setProvisioningUI(ui);
		try {
			java.lang.reflect.Field f = RevertProfilePage.class.getDeclaredField("profileId");
			f.setAccessible(true);
			f.set(page, TESTPROFILE);
		} catch (ReflectiveOperationException e) {
			fail("Could not set profileId via reflection: " + e.getMessage());
		}
		registry = ProvUI.getProfileRegistry(ui.getSession());
	}

	public void testUpdateMovesIUToUpdatedCategory() throws Exception {
		RollbackProfileElement older = latestSnapshot();

		IInstallableUnit top1v2 = createIU(TOPLEVELIU, Version.createOSGi(2, 0, 0));
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.remove(top1);
		req.add(top1v2);
		req.setInstallableUnitProfileProperty(top1v2, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		applyRequest(req);

		Object[] cats = diff(older, latestSnapshot());

		assertEquals(1, cats.length);
		HistoryDiffCategoryElement updated = categoryFor(cats, ChangeType.UPDATED);
		assertTrue(idsIn(updated).contains(TOPLEVELIU));
		HistoryDiffElement el = (HistoryDiffElement) updated.getChildren(updated)[0];
		assertEquals(Version.createOSGi(2, 0, 0), el.getIU().getVersion());
		assertNotNull(el.getPreviousIU());
		assertEquals(Version.create("1.0.0"), el.getPreviousIU().getVersion());
	}

	public void testUnchangedIUDoesNotAppearInDiff() throws Exception {
		RollbackProfileElement older = latestSnapshot();

		install(createIU("org.example.extra", Version.create("1.0.0")), true, false);

		Object[] cats = diff(older, latestSnapshot());

		for (Object obj : cats) {
			Set<String> ids = idsIn((HistoryDiffCategoryElement) obj);
			assertFalse(ids.contains(TOPLEVELIU));
			assertFalse(ids.contains(TOPLEVELIU2));
		}
	}

	public void testSameIdDifferentVersionAppearsOnceAsUpdated() throws Exception {
		RollbackProfileElement older = latestSnapshot();

		IInstallableUnit top1v3 = createIU(TOPLEVELIU, Version.createOSGi(3, 0, 0));
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.remove(top1);
		req.add(top1v3);
		req.setInstallableUnitProfileProperty(top1v3, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		applyRequest(req);

		Object[] cats = diff(older, latestSnapshot());

		long inUpdated = Arrays.stream(cats).map(c -> (HistoryDiffCategoryElement) c)
				.filter(c -> idsIn(c).contains(TOPLEVELIU))
				.filter(c -> ((HistoryDiffElement) c.getChildren(c)[0]).getChangeType() == ChangeType.UPDATED).count();
		assertEquals(1, inUpdated);

		long elsewhere = Arrays.stream(cats).map(c -> (HistoryDiffCategoryElement) c)
				.filter(c -> idsIn(c).contains(TOPLEVELIU))
				.filter(c -> ((HistoryDiffElement) c.getChildren(c)[0]).getChangeType() != ChangeType.UPDATED).count();
		assertEquals(0, elsewhere);
	}

	public void testInstallAndRemoveProducesBothCategories() throws Exception {
		RollbackProfileElement older = latestSnapshot();

		IInstallableUnit newIU = createIU("org.example.brand.new", Version.create("1.0.0"));
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.remove(top2);
		req.add(newIU);
		req.setInstallableUnitProfileProperty(newIU, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		applyRequest(req);

		Object[] cats = diff(older, latestSnapshot());

		assertEquals(2, cats.length);
		assertTrue(idsIn(categoryFor(cats, ChangeType.ADDED)).contains("org.example.brand.new"));
		assertTrue(idsIn(categoryFor(cats, ChangeType.REMOVED)).contains(TOPLEVELIU2));
	}

	// --- helpers ---

	private RollbackProfileElement latestSnapshot() {
		long[] ts = registry.listProfileTimestamps(TESTPROFILE);
		return new RollbackProfileElement(null, TESTPROFILE, ts[ts.length - 1]);
	}

	private Object[] diff(RollbackProfileElement older, RollbackProfileElement newer) {
		try {
			java.lang.reflect.Method m = RevertProfilePage.class.getDeclaredMethod("computeDiff",
					RollbackProfileElement.class, RollbackProfileElement.class,
					org.eclipse.core.runtime.IProgressMonitor.class);
			m.setAccessible(true);
			return (Object[]) m.invoke(page, older, newer, new NullProgressMonitor());
		} catch (ReflectiveOperationException e) {
			fail("Could not invoke computeDiff via reflection: " + e.getMessage());
			return new Object[0];
		}
	}

	private HistoryDiffCategoryElement categoryFor(Object[] categories, ChangeType type) {
		for (Object obj : categories) {
			if (obj instanceof HistoryDiffCategoryElement cat) {
				Object[] children = cat.getChildren(cat);
				if (children.length > 0 && children[0] instanceof HistoryDiffElement el && el.getChangeType() == type) {
					return cat;
				}
			}
		}
		fail("No category found for ChangeType." + type + " in: "
				+ Arrays.stream(categories).map(Object::toString).collect(Collectors.joining(", ")));
		return null;
	}

	private Set<String> idsIn(HistoryDiffCategoryElement cat) {
		Set<String> ids = new HashSet<>();
		for (Object child : cat.getChildren(cat)) {
			if (child instanceof HistoryDiffElement el) {
				ids.add(el.getIU().getId());
			}
		}
		return ids;
	}

	private void applyRequest(ProfileChangeRequest req) throws Exception {
		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		ctx.setMetadataRepositories();
		IProvisioningPlan plan = getPlanner(getSession().getProvisioningAgent()).getProvisioningPlan(req, ctx,
				new NullProgressMonitor());
		assertFalse(plan.getStatus().matches(IStatus.ERROR));
		getSession().performProvisioningPlan(plan, PhaseSetFactory.createDefaultPhaseSet(),
				new ProvisioningContext(getAgent()), new NullProgressMonitor());
	}
}
