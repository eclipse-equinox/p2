/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.persistence;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.importexport.IUDetail;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class P2FWriter extends XMLWriter implements P2FConstants {

	public P2FWriter(OutputStream output, ProcessingInstruction[] piElements) throws UnsupportedEncodingException {
		super(output, piElements);
	}

	public void write(List<IUDetail> ius) {
		start(P2F_ELEMENT);
		attribute(VERSION_ATTRIBUTE, CURRENT_VERSION);
		writeIUs(ius);
		end(P2F_ELEMENT);
		flush();
	}

	private void writeIUs(List<IUDetail> ius) {
		start(IUS_ELEMENT);
		attributeOptional(COLLECTION_SIZE_ATTRIBUTE, String.valueOf(ius.size()));
		for (IUDetail iu : ius)
			writeIU(iu);
		end(IUS_ELEMENT);
	}

	private void writeIU(IUDetail iu) {
		IInstallableUnit unit = iu.getIU();
		start(IU_ELEMENT);
		attribute(ID_ATTRIBUTE, unit.getId());
		attribute(NAME_ATTRIBUTE, unit.getProperty(IInstallableUnit.PROP_NAME, Locale.getDefault().toString()));
		attribute(VERSION_ATTRIBUTE, unit.getVersion().toString());
		start(REPOSITORIES_ELEMENT);
		attribute(COLLECTION_SIZE_ATTRIBUTE, iu.getReferencedRepositories().size());
		for (URI uri : iu.getReferencedRepositories()) {
			start(REPOSITORY_ELEMENT);
			String unencoded = URIUtil.toUnencodedString(uri);
			attribute(LOCATION_ELEMENT, unencoded);
			end(REPOSITORY_ELEMENT);
		}
		end(REPOSITORIES_ELEMENT);
		end(IU_ELEMENT);
	}
}
