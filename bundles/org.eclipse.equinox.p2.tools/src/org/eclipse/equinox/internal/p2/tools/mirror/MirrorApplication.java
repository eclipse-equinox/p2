/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.tools.mirror;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.URIUtil;

/**
 * An application that performs mirroring of artifacts between repositories.
 */
public class MirrorApplication implements IApplication {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String COMMA_SEPARATOR = ","; //$NON-NLS-1$
	private String[] iuSpecs;
	private String[] artifactSpecs;
	private URI metadataSourceLocation;
	private URI metadataDestinationLocation;
	private URI artifactSourceLocation;
	private URI artifactDestinationLocation;
	private boolean referencedIUs = false;
	private boolean raw = false;
	private boolean overwrite = false;
	private boolean verbose = false;
	private boolean compressed = false;

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 */
	public static String[] getArrayArgsFromString(String list, String separator) {
		if (list == null || list.trim().equals(EMPTY_STRING))
			return new String[0];
		List result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals(EMPTY_STRING)) {
				if ((token.indexOf('[') >= 0 || token.indexOf('(') >= 0) && tokens.hasMoreTokens())
					result.add(token + separator + tokens.nextToken());
				else
					result.add(token);
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	private static String[] getArrayArgsFromString(String list, String delimiterStart, String delimiterEnd, String separator) {
		if (list == null || list.trim().equals(EMPTY_STRING))
			return new String[0];
		List result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(list, delimiterStart); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (token.endsWith(delimiterEnd))
				token = token.substring(0, token.length() - delimiterEnd.length());
			if (token.endsWith(delimiterEnd + separator))
				token = token.substring(0, token.length() - delimiterEnd.length() - separator.length());
			if (!token.equals(EMPTY_STRING))
				result.add(token);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public Object start(IApplicationContext context) throws Exception {
		long time = -System.currentTimeMillis();
		initializeFromArguments((String[]) context.getArguments().get("application.args")); //$NON-NLS-1$
		RepositoryMirroring operation = new RepositoryMirroring(metadataSourceLocation, metadataDestinationLocation, artifactSourceLocation, artifactDestinationLocation, overwrite, compressed);
		operation.setReferencedIUs(referencedIUs);
		operation.setRaw(raw);
		operation.setVerbose(verbose);
		operation.mirror(iuSpecs, artifactSpecs);
		time += System.currentTimeMillis();
		if (verbose)
			System.out.println("Operation completed in " + new Long(time) + " ms."); //$NON-NLS-1$//$NON-NLS-2$
		return IApplication.EXIT_OK;
	}

	public void stop() {
		//do nothing
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)
			if (args[i].equalsIgnoreCase("-referencedIUs")) //$NON-NLS-1$
				referencedIUs = true;
			if (args[i].equalsIgnoreCase("-raw")) //$NON-NLS-1$
				raw = true;
			if (args[i].equalsIgnoreCase("-overwrite")) //$NON-NLS-1$
				overwrite = true;
			if (args[i].equalsIgnoreCase("-verbose")) //$NON-NLS-1$
				verbose = true;
			if (args[i].equalsIgnoreCase("-compressed")) //$NON-NLS-1$
				compressed = true;

			// check for args with parameters. If we are at the last argument or 
			// if the next one has a '-' as the first character, then we can't have 
			// an arg with a param so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			try {
				if (args[i - 1].equalsIgnoreCase("-source")) { //$NON-NLS-1$
					metadataSourceLocation = URIUtil.fromString(arg);
					artifactSourceLocation = URIUtil.fromString(arg);
				}
				if (args[i - 1].equalsIgnoreCase("-destination")) { //$NON-NLS-1$
					metadataDestinationLocation = URIUtil.fromString(arg);
					artifactDestinationLocation = URIUtil.fromString(arg);
				}
				if (args[i - 1].equalsIgnoreCase("-metadatadestination")) //$NON-NLS-1$
					metadataDestinationLocation = URIUtil.fromString(arg);
				if (args[i - 1].equalsIgnoreCase("-metadatasource")) //$NON-NLS-1$
					metadataSourceLocation = URIUtil.fromString(arg);
				if (args[i - 1].equalsIgnoreCase("-artifactdestination")) //$NON-NLS-1$
					artifactDestinationLocation = URIUtil.fromString(arg);
				if (args[i - 1].equalsIgnoreCase("-artifactsource")) //$NON-NLS-1$
					artifactSourceLocation = URIUtil.fromString(arg);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Repository location (" + arg + ") must be a URL."); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if (args[i - 1].equalsIgnoreCase("-ius")) //$NON-NLS-1$
				if (arg.equalsIgnoreCase("all")) //$NON-NLS-1$ 
					iuSpecs = new String[0];
				else
					iuSpecs = getArrayArgsFromString(arg, COMMA_SEPARATOR);
			if (args[i - 1].equalsIgnoreCase("-artifacts")) //$NON-NLS-1$
				if (arg.equalsIgnoreCase("all")) //$NON-NLS-1$ 
					artifactSpecs = new String[0];
				else
					artifactSpecs = getArrayArgsFromString(arg, "{", "}", COMMA_SEPARATOR); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
