/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

//TODO Javadoc
/**
 * @since 2.3
 */
public class Remedy {

	private RemedyConfig config;
	private ProfileChangeRequest request;
	private int beingInstalledRelaxedWeight;
	private int installationRelaxedWeight;

	public RemedyConfig getConfig() {
		return config;
	}

	public void setConfig(RemedyConfig config) {
		this.config = config;
	}

	public ProfileChangeRequest getRequest() {
		return request;
	}

	public void setRequest(ProfileChangeRequest request) {
		this.request = request;
	}

	public int getBeingInstalledRelaxedWeight() {
		return beingInstalledRelaxedWeight;
	}

	public void setBeingInstalledRelaxedWeight(int beingInstalledRelaxedWeight) {
		this.beingInstalledRelaxedWeight = beingInstalledRelaxedWeight;
	}

	public int getInstallationRelaxedWeight() {
		return installationRelaxedWeight;
	}

	public void setInstallationRelaxedWeight(int installationRelaxedWeight) {
		this.installationRelaxedWeight = installationRelaxedWeight;
	}

}
