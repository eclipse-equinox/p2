/*******************************************************************************
 * Copyright (c) 2008, 2012 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   SAP AG - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.publishing.Activator;

public class ProductAction extends AbstractPublisherAction {
	protected String source;
	protected String id;
	protected Version version;
	protected String name;
	protected String executableName;
	protected String flavor;
	protected boolean start = false;
	//protected String productLocation;
	protected File executablesFeatureLocation;
	protected IProductDescriptor product;
	protected File jreLocation;
	protected IPublisherResult publisherResults;
	protected MultiStatus finalStatus;

	public ProductAction(String source, IProductDescriptor product, String flavor, File executablesFeatureLocation, File jreLocation) {
		super();
		this.source = source;
		this.flavor = flavor;
		this.executablesFeatureLocation = executablesFeatureLocation;
		this.product = product;
		this.jreLocation = jreLocation;
		//this.productLocation = productLocation;
	}

	public ProductAction(String source, IProductDescriptor product, String flavor, File executablesFeatureLocation) {
		this(source, product, flavor, executablesFeatureLocation, null);
	}

	protected IPublisherAction[] createActions(IPublisherResult results) {
		// generate the advice we can up front.
		createAdvice();

		// create all the actions needed to publish a product
		ArrayList<IPublisherAction> actions = new ArrayList<IPublisherAction>();
		// products include the executable so add actions to publish them
		if (getExecutablesLocation() != null && this.product.includeLaunchers())
			actions.add(createApplicationExecutableAction(info.getConfigurations()));
		// add the actions that just configure things.
		actions.add(createConfigCUsAction());
		actions.add(createJREAction());
		actions.add(createDefaultCUsAction());
		actions.add(createRootIUAction());
		return actions.toArray(new IPublisherAction[actions.size()]);
	}

	protected IPublisherAction createApplicationExecutableAction(String[] configSpecs) {
		return new ApplicationLauncherAction(id, version, flavor, executableName, getExecutablesLocation(), configSpecs);
	}

	protected IPublisherAction createDefaultCUsAction() {
		return new DefaultCUsAction(info, flavor, 4, false);
	}

	protected IPublisherAction createRootIUAction() {
		return new RootIUAction(id, version, name);
	}

	protected IPublisherAction createConfigCUsAction() {
		return new ConfigCUsAction(info, flavor, id, version);
	}

	protected IPublisherAction createJREAction() {
		return new JREAction(jreLocation);
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		monitor = SubMonitor.convert(monitor);
		this.info = publisherInfo;
		publisherResults = results;
		finalStatus = new MultiStatus(Activator.ID, 0, NLS.bind(Messages.message_problemPublishingProduct, product.getId()), null);
		IPublisherAction[] actions = createActions(results);
		for (int i = 0; i < actions.length; i++) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			finalStatus.merge(actions[i].perform(publisherInfo, results, monitor));
		}
		if (!finalStatus.isOK())
			return finalStatus;

		return Status.OK_STATUS;
	}

	private void createAdvice() {
		executableName = product.getLauncherName();
		createProductAdvice();
		createAdviceFileAdvice();
		createRootAdvice();
		info.addAdvice(new RootIUResultFilterAdvice(null));
	}

	/**
	 * Create advice for a p2.inf file co-located with the product file, if any.
	 */
	private void createAdviceFileAdvice() {
		File productFileLocation = product.getLocation();
		if (productFileLocation == null)
			return;

		AdviceFileAdvice advice = new AdviceFileAdvice(product.getId(), Version.parseVersion(product.getVersion()), new Path(productFileLocation.getParent()), new Path("p2.inf")); //$NON-NLS-1$
		if (advice.containsAdvice())
			info.addAdvice(advice);
	}

	private void createRootAdvice() {
		Collection<IVersionedId> list;
		switch (product.getProductContentType()) { // add new case for each new content type included in product
			case MIXED : // include all content types
				list = versionElements(listElements(product.getFeatures(), ".feature.group"), IInstallableUnit.NAMESPACE_IU_ID); //$NON-NLS-1$
				list.addAll(versionElements(listElements(product.getBundles(true), null), IInstallableUnit.NAMESPACE_IU_ID));
				break;
			case FEATURES : // include features only
				list = versionElements(listElements(product.getFeatures(), ".feature.group"), IInstallableUnit.NAMESPACE_IU_ID); //$NON-NLS-1$

				if (product.hasBundles(true)) {
					finalStatus.add(new Status(IStatus.INFO, Activator.ID, Messages.bundlesInProductFileIgnored));
				}
				break;
			case BUNDLES : // include bundles only
				list = versionElements(listElements(product.getBundles(true), null), IInstallableUnit.NAMESPACE_IU_ID);

				if (product.hasFeatures()) {
					finalStatus.add(new Status(IStatus.INFO, Activator.ID, Messages.featuresInProductFileIgnored));
				}
				break;
			default :
				throw new IllegalStateException(NLS.bind(Messages.exception_invalidProductContentType, product.getProductContentType().toString(), ProductContentType.getAllowedSetOfValues().toString()));
		}

		info.addAdvice(new RootIUAdvice(list));
	}

	private void createProductAdvice() {
		id = product.getId();
		version = Version.parseVersion(product.getVersion());
		name = product.getProductName();
		if (name == null || name.length() == 0) // If the name is not defined, use the ID
			name = product.getId();

		String[] configSpecs = info.getConfigurations();
		for (int i = 0; i < configSpecs.length; i++)
			info.addAdvice(new ProductFileAdvice(product, configSpecs[i]));
	}

	private Collection<IVersionedId> versionElements(Collection<IVersionedId> elements, String namespace) {
		Collection<IVersionAdvice> versionAdvice = info.getAdvice(null, true, null, null, IVersionAdvice.class);
		List<IVersionedId> result = new ArrayList<IVersionedId>();
		for (IVersionedId element : elements) {
			Version elementVersion = element.getVersion();
			if (elementVersion == null || Version.emptyVersion.equals(elementVersion)) {
				Iterator<IVersionAdvice> advice = versionAdvice.iterator();
				while (advice.hasNext()) {
					elementVersion = advice.next().getVersion(namespace, element.getId());
					break;
				}
			}

			//	if advisedVersion is null, we get the highest version
			IInstallableUnit unit = queryForIU(publisherResults, element.getId(), elementVersion);
			if (unit != null) {
				result.add(unit);
			} else {
				// if the bundle is platform specific we will have broken metadata due to a missing filter
				finalStatus.add(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.message_cannotDetermineFilterOnInclusion, element.getId(), elementVersion)));

				// preserve legacy behaviour for callers who choose to ignore the error status:
				// include element without filter, but only if there was an IVersionAdvice with "no advice" for this bundle (see bug 398066)
				if (elementVersion != null) {
					result.add(new VersionedId(element.getId(), elementVersion));
				}
			}
		}
		return result;
	}

	private Collection<IVersionedId> listElements(List<IVersionedId> elements, String suffix) {
		if (suffix == null || suffix.length() == 0)
			return elements;
		ArrayList<IVersionedId> result = new ArrayList<IVersionedId>(elements.size());
		for (IVersionedId elementName : elements) {
			result.add(new VersionedId(elementName.getId() + suffix, elementName.getVersion()));
		}
		return result;
	}

	protected File getExecutablesLocation() {
		if (executablesFeatureLocation != null)
			return executablesFeatureLocation;
		if (source != null)
			return new File(source);
		return null;
	}
}
