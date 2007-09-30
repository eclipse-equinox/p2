/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.prov.resolution;

import java.util.*;
import org.eclipse.equinox.internal.prov.metadata.InternalInstallableUnit;
import org.eclipse.equinox.internal.prov.metadata.MetadataActivator;
import org.eclipse.equinox.prov.metadata.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.ServiceReference;

public class ResolutionHelper {
	private static boolean DEBUG = false;

	private Transformer transformer;
	private State state;
	private Dictionary selectionContext;
	private RecommendationDescriptor recommendations;

	public ResolutionHelper(Dictionary selectionContext, RecommendationDescriptor recommendations) {
		this.selectionContext = selectionContext;
		this.recommendations = recommendations;
	}

	private void initialize() {
		ServiceReference sr = MetadataActivator.context.getServiceReference(PlatformAdmin.class.getName());
		PlatformAdmin pa = (PlatformAdmin) MetadataActivator.context.getService(sr);
		transformer = new Transformer(pa.getFactory(), selectionContext, recommendations);
		state = pa.getFactory().createState(true);
		if (selectionContext != null)
			state.setPlatformProperties(selectionContext);
	}

	private void addToState(BundleDescription bd) {
		state.addBundle(bd);
	}

	private BundleDescription addInResolution(IInstallableUnit toAdd) {
		toAdd.accept(transformer);
		//		transformer.visitInstallableUnit(toAdd);
		BundleDescription descriptionToAdd = transformer.getResult();
		//		bundleDescriptionToIU.put(descriptionToAdd, toAdd);
		addToState(descriptionToAdd);
		return descriptionToAdd;
	}

	/** 
	 * Indicates if the installable unit to install will have all their constraints satisfied when installed with other installable units.
	 * @param toInstall the installable units to install
	 * @param existingState the other installable units to resolve against
	 * @return true if the installable unit to install resolves, return false otherwise. 
	 */
	public UnsatisfiedCapability[] install(Set toInstall, Set existingState) {
		initialize();
		BundleDescription[] addedBundle = new BundleDescription[toInstall.size()];
		int j = 0;
		for (Iterator iterator = toInstall.iterator(); iterator.hasNext();) {
			addedBundle[j++] = addInResolution((IInstallableUnit) iterator.next());
		}

		for (Iterator iterator = existingState.iterator(); iterator.hasNext();) {
			addInResolution((IInstallableUnit) iterator.next());
		}

		state.resolve(); //We may want to resolve in two times. first the existing state, then add the toInstall. This would allow to see what changes when dropping the new iu.
		//Also it could allow us to do an incremental resolution. however the results may differ

		ArrayList results = new ArrayList();
		for (int i = 0; i < addedBundle.length; i++) {
			results.addAll(createUnsatisfiedCapabilities(state.getStateHelper().getUnsatisfiedConstraints(addedBundle[i]), addedBundle[i]));
		}
		return (UnsatisfiedCapability[]) results.toArray(new UnsatisfiedCapability[results.size()]);

	}

	private ArrayList createUnsatisfiedCapabilities(VersionConstraint[] unsatisfied, BundleDescription description) {
		ArrayList results = new ArrayList();
		for (int i = 0; i < unsatisfied.length; i++) {
			RequiredCapability originalDependency = (RequiredCapability) ((StateMetadataMap) description.getUserObject()).getGenericSpecifications().get(unsatisfied[i]);
			results.add(new UnsatisfiedCapability(originalDependency, ((StateMetadataMap) description.getUserObject()).getUnit()));
		}
		return results;
	}

	/**
	 * Associates installable unit fragments to each IU being installed.
	 * 
	 * TODO: This method should probably be renamed to attachFragments
	 */
	public Collection attachCUs(Collection toAttach) {
		initialize();
		Collection result = new HashSet(toAttach.size());
		for (Iterator iterator = toAttach.iterator(); iterator.hasNext();) {
			IResolvedInstallableUnit tmp = ((InternalInstallableUnit) iterator.next()).getResolved();
			result.add(tmp);
			addInResolution(tmp);
		}
		state.resolve();
		BundleDescription[] bds = state.getBundles();
		for (int i = 0; i < bds.length; i++) {
			if (DEBUG) {
				ResolverError[] re = state.getResolverErrors(bds[i]);
				for (int j = 0; j < re.length; j++) {
					System.out.println(re[j]);
				}
			}
			BundleDescription[] potentialIUFragments = state.getStateHelper().getDependentBundles(new BundleDescription[] {bds[i]});
			// TODO: We need to define a way to allow IUFragments to work together to do configuration work.
			// For now we will select just one fragment by preferring a fragment that matches the host

			IInstallableUnit hostIU = ((StateMetadataMap) bds[i].getUserObject()).getUnit();
			IResolvedInstallableUnitFragment selectedFragment = null;
			for (int k = 0; k < potentialIUFragments.length; k++) {
				IInstallableUnit dependentIU = ((StateMetadataMap) potentialIUFragments[k].getUserObject()).getUnit();
				if (hostIU.equals(dependentIU))
					continue;

				if (dependentIU.isFragment()) {
					IResolvedInstallableUnitFragment potentialFragment = (IResolvedInstallableUnitFragment) dependentIU;

					if (potentialFragment.getHostId() == null) {
						// default fragment - we'll mark it selected but keep looking for a fragment that matches the host
						selectedFragment = potentialFragment;
					} else if (potentialFragment.getHostId().equals(hostIU.getId()) && potentialFragment.getHostVersionRange().isIncluded(hostIU.getVersion())) {
						// matches host - we're done
						selectedFragment = potentialFragment;
						break;
					} // otherwise keep looking
				}
			}
			if (selectedFragment != null)
				((ResolvedInstallableUnit) hostIU).setFragments(new IResolvedInstallableUnit[] {selectedFragment});
		}
		return result;
	}

	public boolean isResolved(IInstallableUnit iu) {
		return state.getBundle(iu.getId(), iu.getVersion()).isResolved();
	}

	public ArrayList getAllResolved() {
		BundleDescription[] bd = state.getResolvedBundles();
		ArrayList result = new ArrayList(bd.length);
		for (int i = 0; i < bd.length; i++) {
			result.add(extractIU(bd[i]));
		}
		return result;
	}

	private IInstallableUnit extractIU(BundleDescription bd) {
		return ((StateMetadataMap) bd.getUserObject()).getUnit();
	}

	public List getSorted() {
		BundleDescription[] toSort = state.getResolvedBundles();
		state.getStateHelper().sortBundles(toSort);
		List result = new ArrayList(toSort.length);
		for (int i = 0; i < toSort.length; i++) {
			result.add(extractIU(toSort[i]));
		}
		return result;
	}
}
