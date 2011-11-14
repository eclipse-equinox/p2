/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * Sonatype, Inc. - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class IULoader {

	private static Annotation[] annos;

	public static void loadIUs(Object o) {
		Class<? extends Object> classWithIUs = o.getClass();
		annos = classWithIUs.getAnnotations();
		for (int i = 0; i < annos.length; i++) {
			System.out.println(annos[i]);
		}

		Field[] fields = classWithIUs.getFields();
		for (int i = 0; i < fields.length; i++) {
			Annotation[] a = fields[i].getAnnotations();
			for (int j = 0; j < a.length; j++) {
				if (a[j] instanceof IUDescription) {
					IUDescription ml = (IUDescription) a[j]; // here it is !!!
					ReducedCUDFParser parser = new ReducedCUDFParser();
					InputStream is = new ByteArrayInputStream(ml.content().getBytes());
					parser.parse(is, false, null);
					try {
						fields[i].set(o, parser.getIU());
						is.close();
					} catch (IllegalArgumentException e) {
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}
}
