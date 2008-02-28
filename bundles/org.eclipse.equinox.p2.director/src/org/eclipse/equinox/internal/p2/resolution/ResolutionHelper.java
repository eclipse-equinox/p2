/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.resolution;

import java.util.*;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.RecommendationDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.ServiceReference;

public class ResolutionHelper {
	private static final IInstallableUnitFragment[] NO_FRAGMENTS = new IInstallableUnitFragment[0];

	private static boolean DEBUG = false;

	private Transformer transformer;
	private State state;
	private Dictionary selectionContext;
	private RecommendationDescriptor recommendations;
	/**
	 * Map of IInstallableUnit->(IInstallableUnitFragment) representing the 
	 * mapping of IUs to the fragment they are bound to.
	 */
	private Map fragmentBindings;

	public ResolutionHelper(Dictionary selectionContext, RecommendationDescriptor recommendations) {
		this.selectionContext = selectionContext;
		this.recommendations = recommendations;
	}

	private void initialize() {
		ServiceReference sr = DirectorActivator.context.getServiceReference(PlatformAdmin.class.getName());
		PlatformAdmin pa = (PlatformAdmin) DirectorActivator.context.getService(sr);
		transformer = new Transformer(pa.getFactory(), selectionContext, recommendations);
		state = pa.getFactory().createState(true);
		fragmentBindings = new HashMap();
		if (selectionContext != null)
			state.setPlatformProperties(selectionContext);
	}

	private void addToState(BundleDescription bd) {
		state.addBundle(bd);
	}

	private BundleDescription addInResolution(IInstallableUnit toAdd) {
		transformer.visitInstallableUnit(toAdd);
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
		for (Iterator iterator = toAttach.iterator(); iterator.hasNext();) {
			addInResolution((IInstallableUnit) iterator.next());
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
			ArrayList applicableFragments = new ArrayList();
			for (int k = 0; k < potentialIUFragments.length; k++) {
				IInstallableUnit dependentIU = ((StateMetadataMap) potentialIUFragments[k].getUserObject()).getUnit();
				if (hostIU.equals(dependentIU) || !dependentIU.isFragment())
					continue;

				IInstallableUnitFragment potentialFragment = (IInstallableUnitFragment) dependentIU;

				// Check to make sure the host meets the requirements of the fragment
				RequiredCapability reqsFromFragment[] = potentialFragment.getHost();
				ProvidedCapability hostCapabilities[] = hostIU.getProvidedCapabilities();
				boolean match = true;
				boolean requirementMatched = false;
				for (int l = 0; l < reqsFromFragment.length && match == true; l++) {
					requirementMatched = false;
					for (int m = 0; m < hostCapabilities.length; m++) {
						if (hostCapabilities[m].isSatisfiedBy(reqsFromFragment[l])) {
							requirementMatched = true;
							break;
						}
					}
					if (requirementMatched == false) {
						match = false;
						break;
					}

				}
				if (match) {
					applicableFragments.add(potentialFragment);
				}
			}

			IInstallableUnitFragment theFragment = null;
			int specificityLevel = 0;
			for (Iterator iterator = applicableFragments.iterator(); iterator.hasNext();) {
				IInstallableUnitFragment fragment = (IInstallableUnitFragment) iterator.next();
				if (fragment.getHost().length > specificityLevel) {
					theFragment = fragment;
					specificityLevel = fragment.getHost().length;
				}
			}
			if (theFragment != null)
				fragmentBindings.put(hostIU, theFragment);
		}
		//build the collection of resolved IUs
		Collection result = new HashSet(toAttach.size());
		for (Iterator iterator = toAttach.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			//just return fragments as they are
			if (iu.isFragment()) {
				result.add(iu);
				continue;
			}
			//return a new IU that combines the IU with its bound fragments
			IInstallableUnitFragment fragment = (IInstallableUnitFragment) fragmentBindings.get(iu);
			IInstallableUnitFragment[] fragments;
			if (fragment == null)
				fragments = NO_FRAGMENTS;
			else
				fragments = new IInstallableUnitFragment[] {fragment};
			result.add(MetadataFactory.createResolvedInstallableUnit(iu, fragments));
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
