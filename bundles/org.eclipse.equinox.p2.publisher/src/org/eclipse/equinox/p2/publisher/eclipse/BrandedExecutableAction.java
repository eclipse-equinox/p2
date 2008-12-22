/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import org.eclipse.equinox.internal.p2.publisher.eclipse.*;
import org.eclipse.equinox.internal.provisional.p2.core.Version;

public class BrandedExecutableAction extends EquinoxExecutableAction {

	private ProductFile product;

	public BrandedExecutableAction(ExecutablesDescriptor executables, String productLocation, String flavor, String configSpec) {
		super();
		this.executables = executables;
		this.flavor = flavor;
		this.configSpec = configSpec;
		product = loadProduct(productLocation);
		idBase = product.getProductName();
		version = new Version(product.getVersion());
	}

	private ProductFile loadProduct(String productLocation) {
		ProductFile result = null;
		try {
			result = new ProductFile(productLocation);
		} catch (Exception e) {
			//TODO
		}
		if (result == null)
			throw new IllegalArgumentException("unable to load product file");
		return result;
	}

	/**
	 * Brands a copy of the given executable descriptor with the information in the 
	 * current product definition.  The files described in the descriptor are also copied
	 * to a temporary location to avoid destructive modification
	 * @param descriptor the executable descriptor to brand.
	 * @return the new descriptor
	 */
	protected ExecutablesDescriptor brandExecutables(ExecutablesDescriptor descriptor) {
		ExecutablesDescriptor result = new ExecutablesDescriptor(descriptor);
		result.makeTemporaryCopy();
		BrandingIron iron = new BrandingIron();
		String os = parseConfigSpec(configSpec)[1];
		iron.setIcons(product.getIcons(os));
		iron.setName(product.getLauncherName());
		iron.setOS(os);
		iron.setRoot(result.getLocation().getAbsolutePath());
		try {
			iron.brand();
			result.setExecutableName(product.getLauncherName(), true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
}
