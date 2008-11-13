/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.policy;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * The Policy class is used to locate application specific policies that
 * should be used in the standard p2 UI class libraries.  
 * 
 * Policy allows clients to specify things such as how repositories 
 * are manipulated in the standard wizards and dialogs, and how the repositories
 * or the installation itself should be traversed when displaying content.
 * 
 * In some cases, Policy defines a default policy that is overridden
 * by user choice and subsequently stored in dialog settings.
 * 
 * Client applications should generally configure the Policy when their
 * application's bundle is started.  
 * 
 * @since 3.5
 */

public class Policy {

	private static Policy defaultInstance;

	private QueryProvider queryProvider;
	private LicenseManager licenseManager;
	private PlanValidator planValidator;
	private IProfileChooser profileChooser;
	private IUViewQueryContext queryContext;
	private RepositoryManipulator repositoryManipulator;

	public static Policy getDefault() {
		if (defaultInstance == null)
			defaultInstance = new Policy();
		return defaultInstance;
	}

	private static IStatus getDefaultPolicyAlreadySetStatus() {
		return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.Policy_CannotResetDefaultPolicy);

	}

	public static void setDefaultPolicy(Policy policy) {
		if (defaultInstance != null) {
			ProvUI.reportStatus(getDefaultPolicyAlreadySetStatus(), StatusManager.LOG);
			return;
		}
		defaultInstance = policy;
	}

	/**
	 * Returns the query provider used to query for the UI elements shown
	 * in the various UI components.
	 * 
	 * @return the queryProvider
	 */
	public QueryProvider getQueryProvider() {
		if (queryProvider == null) {
			queryProvider = getDefaultQueryProvider();
		}
		return queryProvider;
	}

	/**
	 * Set the query provider used to query for the UI elements shown in 
	 * the various UI components.
	 * 
	 * @param provider
	 * the provider to use, or <code>null</code> to use the default
	 *            provider
	 */
	public void setQueryProvider(QueryProvider provider) {
		queryProvider = provider;
	}

	/**
	 * Returns the license manager used to remember accepted licenses
	 * 
	 * @return the licenseManager
	 */
	public LicenseManager getLicenseManager() {
		if (licenseManager == null) {
			licenseManager = getDefaultLicenseManager();
		}
		return licenseManager;
	}

	/**
	 * Set the license manager used to remember accepted licenses.
	 * 
	 * @param manager the manager to use, or <code>null</code> to use 
	 * the default manager
	 */
	public void setLicenseManager(LicenseManager manager) {
		licenseManager = manager;
	}

	/**
	 * Returns the plan validator used to validate a proposed provisioning
	 * plan
	 * 
	 * @return the plan validator
	 */
	public PlanValidator getPlanValidator() {
		if (planValidator == null) {
			planValidator = getDefaultPlanValidator();
		}
		return planValidator;
	}

	/**
	 * Set the plan validator used to validate a proposed provisioning
	 * plan
	 * 
	 * @param validator the validator to use, or <code>null</code> to use 
	 * the default validator
	 */
	public void setPlanValidator(PlanValidator validator) {
		planValidator = validator;
	}

	/**
	 * Get the profile chooser used to provide a profile id when performing
	 * operations on a profile and the profile id is not otherwise specified.
	 * 
	 * @return the profile chooser
	 */
	public IProfileChooser getProfileChooser() {
		if (profileChooser == null) {
			return getDefaultProfileChooser();
		}
		return profileChooser;
	}

	/**
	 * Set the profile chooser used to provide a profile id when performing
	 * operations on a profile and the profile id is not otherwise specified.
	 * 
	 * @param chooser the chooser to use, or <code>null</code> to use 
	 * the default chooser
	 */
	public void setProfileChooser(IProfileChooser chooser) {
		profileChooser = chooser;
	}

	/**
	 * Get the query context that is used to drive the filtering and 
	 * traversal of any IU views
	 * 
	 * @return the queryContext
	 */
	public IUViewQueryContext getQueryContext() {
		if (queryContext == null) {
			return getDefaultQueryContext();
		}
		return queryContext;
	}

	/**
	 * Set the query context that is used to drive the filtering and
	 * traversal of any IU views
	 * 
	 * @param context the context to use, or <code>null</code> to use 
	 * the default context
	 */
	public void setQueryContext(IUViewQueryContext context) {
		queryContext = context;
	}

	/**
	 * Get the repository manipulator that is used to perform repository
	 * operations given a URL.
	 * 
	 * @return the repository manipulator
	 */
	public RepositoryManipulator getRepositoryManipulator() {
		if (repositoryManipulator == null) {
			return getDefaultRepositoryManipulator();
		}
		return repositoryManipulator;
	}

	/**
	 * Set the repository manipulator that is used to perform repository
	 * operations given a URL.
	 * 
	 * @param manipulator the manipulator to use, or <code>null</code> to use 
	 * the default manipulator
	 */
	public void setRepositoryManipulator(RepositoryManipulator manipulator) {
		repositoryManipulator = manipulator;
	}

	/**
	 * Reset all of the policies to their default values
	 */
	public void reset() {
		licenseManager = null;
		planValidator = null;
		profileChooser = null;
		queryContext = null;
		queryProvider = null;
		repositoryManipulator = null;
	}

	private RepositoryManipulator getDefaultRepositoryManipulator() {
		return new ColocatedRepositoryManipulator(this);
	}

	/*
	 * Returns the plan validator to use if none has been set.  This
	 * validator approves every plan.
	 */
	private PlanValidator getDefaultPlanValidator() {
		return new PlanValidator() {
			public boolean continueWorkingWithPlan(ProvisioningPlan plan, Shell shell) {
				return true;
			}
		};
	}

	/*
	 * Returns the license manager to use if none has been set.
	 */
	private LicenseManager getDefaultLicenseManager() {
		return new SimpleLicenseManager();
	}

	/*
	 * Returns the profile chooser to use if none has been set.
	 * This profile chooser uses the profile id of the running
	 * application.
	 */
	private IProfileChooser getDefaultProfileChooser() {
		return new IProfileChooser() {
			public String getProfileId(Shell shell) {
				return IProfileRegistry.SELF;
			}
		};
	}

	/*
	 * Returns the query provider used to provide a descriptor for
	 * the various queries that are used to show the UI elements of a
	 * particular installation.  The default returns
	 * a null query descriptor.
	 */
	private QueryProvider getDefaultQueryProvider() {
		return new DefaultQueryProvider(this);
	}

	/*
	 * Returns an IUViewQueryContext with default values
	 */
	private IUViewQueryContext getDefaultQueryContext() {
		return new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_BY_REPO);
	}
}
