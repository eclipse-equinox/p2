/*******************************************************************************
 * Copyright (c) 2011, 2017 Sonatype, Inc. and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Sonatype, Inc. - initial implementation and ideas
 ******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class IULoader {

	private static Annotation[] annos;

	public static void loadIUs(Object o) {
		Class<? extends Object> classWithIUs = o.getClass();
		annos = classWithIUs.getAnnotations();
		for (Annotation anno : annos) {
			System.out.println(anno);
		}

		Field[] fields = classWithIUs.getFields();
		for (Field field : fields) {
			Annotation[] a = field.getAnnotations();
			for (Annotation a1 : a) {
				if (a1 instanceof IUDescription ml) {
					 // here it is !!!
					ReducedCUDFParser parser = new ReducedCUDFParser();
					try (final InputStream is = new ByteArrayInputStream(ml.content().getBytes())) {
						parser.parse(is, false, null);
						field.set(o, parser.getIU());
					}catch (IllegalArgumentException e) {
						throw new RuntimeException(e);
					}catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}
}
