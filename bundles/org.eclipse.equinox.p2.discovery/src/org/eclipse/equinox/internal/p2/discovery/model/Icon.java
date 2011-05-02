/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.model;

/**
 * @author David Green
 */
public class Icon {

	protected String image16;

	protected String image32;

	protected String image48;

	protected String image64;

	protected String image128;

	public Icon() {
	}

	public String getImage16() {
		return image16;
	}

	public void setImage16(String image16) {
		this.image16 = image16;
	}

	public String getImage32() {
		return image32;
	}

	public void setImage32(String image32) {
		this.image32 = image32;
	}

	public String getImage48() {
		return image48;
	}

	public void setImage48(String image48) {
		this.image48 = image48;
	}

	public String getImage64() {
		return image64;
	}

	public void setImage64(String image64) {
		this.image64 = image64;
	}

	public String getImage128() {
		return image128;
	}

	public void setImage128(String image128) {
		this.image128 = image128;
	}

	public void validate() throws ValidationException {
	}

}
