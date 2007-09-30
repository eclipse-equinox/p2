/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.resolution;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.MetadataActivator;
import org.eclipse.equinox.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.InvalidSyntaxException;

public class Transformer implements IMetadataVisitor {
	private static final byte IU_KIND = 0;
	private static final String IU_NAMESPACE = "iu.namespace";
	static long iuInternalId = 0;

	private Dictionary context = null;
	private StateObjectFactory factory = null;
	private ArrayList iuCapabilities;
	private Map iuDependencies;
	private byte kind = IU_KIND;

	private BundleDescription result = null;

	private RecommendationDescriptor recommendations = null;

	public Transformer(StateObjectFactory factory) {
		this(factory, null, null);
	}

	public Transformer(StateObjectFactory factory, Dictionary context, RecommendationDescriptor recommendations) {
		this.factory = factory;
		this.context = context;
		this.recommendations = recommendations;
	}

	private String getNamespace() {
		switch (kind) {
			case IU_KIND :
				return IU_NAMESPACE;
			default :
				throw new IllegalStateException("unknown kind"); //This should not happen
		}
	}

	public BundleDescription getResult() {
		return result;
	}

	private boolean isEnabled(RequiredCapability capability) {
		// If there is no context then be optimistic
		if (context == null)
			return true;
		String filter = capability.getFilter();
		if (filter == null)
			return true;
		try {
			return MetadataActivator.getContext().createFilter(filter).match(context);
		} catch (InvalidSyntaxException e) {
			// If we fail to parse the filter treat it as invalid and be optimistic
			return true;
		}
	}

	private String toFilter(VersionRange range) {
		if (range == null)
			return null;
		StringBuffer buf = new StringBuffer();
		buf.append("(&"); //$NON-NLS-1$
		buf.append("(version>=").append(range.getMinimum().toString()).append(')'); //$NON-NLS-1$
		if (!range.getIncludeMinimum())
			buf.append("(!(version=").append(range.getMinimum().toString()).append("))");
		buf.append("(version<=").append(range.getMaximum().toString()).append(')'); //$NON-NLS-1$
		if (!range.getIncludeMaximum())
			buf.append("(!(version=").append(range.getMaximum().toString()).append("))");
		buf.append(')');
		return buf.toString();
	}

	public void visitCapability(ProvidedCapability capability) {
		iuCapabilities.add(factory.createGenericDescription(capability.getName(), capability.getNamespace(), capability.getVersion(), null));
	}

	public void visitInstallableUnit(IInstallableUnit toTransform) {
		kind = IU_KIND;

		//Start with the dependencies
		RequiredCapability[] requires = toTransform.getRequiredCapabilities();
		iuDependencies = new HashMap(requires.length);
		for (int i = 0; i < requires.length; i++) {
			requires[i].accept(this);
		}

		//Do the capabilities
		ProvidedCapability[] capabilities = toTransform.getProvidedCapabilities();
		iuCapabilities = new ArrayList(requires.length + 1);
		for (int i = 0; i < capabilities.length; i++) {
			capabilities[i].accept(this);
		}

		//Add a capability representing the IU itself
		iuCapabilities.add(factory.createGenericDescription(toTransform.getId(), getNamespace(), toTransform.getVersion(), null));

		GenericSpecification[] genericSpecifications = new GenericSpecification[iuDependencies.size()];
		iuDependencies.keySet().toArray(genericSpecifications);

		GenericDescription[] genericDescriptions = new GenericDescription[iuCapabilities.size()];
		iuCapabilities.toArray(genericDescriptions);

		//Finally create the bundle description
		//TODO Need to create the filter for the IU itself
		result = factory.createBundleDescription(iuInternalId++, toTransform.getId(), toTransform.getVersion(), (String) null, (BundleSpecification[]) null, (HostSpecification) null, (ImportPackageSpecification[]) null, (ExportPackageDescription[]) null, toTransform.isSingleton(), true, true, toTransform.getFilter(), (String[]) null, genericSpecifications, genericDescriptions);
		result.setUserObject(new StateMetadataMap(toTransform, iuDependencies));
	}

	public void visitRequiredCapability(RequiredCapability capability) {
		try {
			if (isEnabled(capability)) {
				capability = rewrite(capability);
				iuDependencies.put(factory.createGenericSpecification(capability.getName(), capability.getNamespace(), toFilter(capability.getRange()), capability.isOptional(), capability.isMultiple()), capability);
			}
		} catch (InvalidSyntaxException e) {
			LogHelper.log(new Status(IStatus.ERROR, MetadataActivator.PI_METADATA, "Invalid filter: " + e.getFilter(), e)); //$NON-NLS-1$
		}
	}

	private RequiredCapability rewrite(RequiredCapability match) {
		if (recommendations == null)
			return match;
		Recommendation foundRecommendation = recommendations.findRecommendation(match);
		return foundRecommendation != null ? foundRecommendation.newValue() : match;
	}
}
