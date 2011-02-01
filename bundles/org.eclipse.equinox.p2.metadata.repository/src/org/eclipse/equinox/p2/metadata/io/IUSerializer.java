/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sonatype, Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.io;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * This class allows to serialize a collection of {@link IInstallableUnit}s.
 * These serialized IUs can be read using the {@link IUDeserializer}.
 * The format in which the IUs are serialized is not API and is subject to change without notice.
 * @since 1.2
 */
public class IUSerializer {
	MetadataWriter writer;

	/**
	 * Construct a serializer.
	 * @param os the output stream against which the serializer will work.
	 * @throws UnsupportedEncodingException
	 */
	public IUSerializer(OutputStream os) throws UnsupportedEncodingException {
		writer = new MetadataWriter(os, null);
	}

	/**
	 * Serialize the given collections of IU.
	 * @param ius the collection of {@link IInstallableUnit}s to serialize
	 */
	public void write(Collection<IInstallableUnit> ius) {
		writer.writeInstallableUnits(ius.iterator(), ius.size());
		writer.flush();
	}
}
