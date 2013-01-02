/*******************************************************************************
 * Copyright (c) 2000-2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.compare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


class CompareUtilities {
	
	
	public static String readString(final IStreamContentAccessor input) {
		try {
			String encoding = null;
			if (input instanceof IEncodedStreamContentAccessor) {
				encoding = ((IEncodedStreamContentAccessor) input).getCharset();
			}
			if (encoding == null) {
				encoding = ResourcesPlugin.getEncoding();
			}
			return readString(input, encoding);
		}
		catch (final CoreException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, -1,
					"An error occurred when loading compare input.", e));
		}
		return ""; //$NON-NLS-1$
	}
	
	public static String readString(final IStreamContentAccessor sca, final String encoding) throws CoreException {
		String s = null;
		try {
			try {
				s = readString(sca.getContents(), encoding);
			}
			catch (final UnsupportedEncodingException e) {
				if (!encoding.equals(ResourcesPlugin.getEncoding())) {
					s = readString(sca.getContents(), ResourcesPlugin.getEncoding());
				}
			}
		}
		catch (final IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, -1, e.getMessage(), e));
		}
		return s;
	}
	
	public static String readString(final InputStream is, final String encoding) throws IOException {
		if (is == null) {
			return null;
		}
		BufferedReader reader= null;
		try {
			final StringBuffer buffer = new StringBuffer();
			final char[] part = new char[2048];
			int read = 0;
			reader = new BufferedReader(new InputStreamReader(is, encoding));
			while ((read = reader.read(part)) != -1) {
				buffer.append(part, 0, read);
			}
			
			return buffer.toString();
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (final IOException ignore) {}
			}
		}
	}
	
}
