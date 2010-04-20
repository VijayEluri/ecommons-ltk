/*******************************************************************************
 * Copyright (c) 2008-2010 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;


/**
 * Problem in a source unit. Default implementation of {@link IProblem}.
 */
public class Problem implements IProblem {
	
	
	private final ISourceUnit fUnit;
	private final int fLine;
	private final int fStart;
	private final int fStop;
	
	private final int fSeverity;
	private final int fCode;
	private final String fMessage;
	
	
	public Problem(final int severity, final int code, final String message,
			final ISourceUnit unit, final int line, final int startOffset, final int stopOffset) {
		fSeverity = severity;
		fCode = code;
		fMessage = message;
		
		fUnit = unit;
		fLine = line;
		fStart = startOffset;
		fStop = (stopOffset - startOffset > 0) ? stopOffset : startOffset + 1;
	}
	
	public Problem(final int severity, final int code, final String message,
			final ISourceUnit unit, final int startOffset, final int stopOffset) {
		fSeverity = severity;
		fCode = code;
		fMessage = message;
		
		fUnit = unit;
		fLine = -1;
		fStart = startOffset;
		fStop = (stopOffset - startOffset > 0) ? stopOffset : startOffset + 1;
	}
	
	
	public ISourceUnit getSourceUnit() {
		return fUnit;
	}
	
	public int getSourceLine() {
		return fLine;
	}
	
	public int getSourceStartOffset() {
		return fStart;
	}
	
	public int getSourceStopOffset() {
		return fStop;
	}
	
	public int getSeverity() {
		return fSeverity;
	}
	
	public int getCode() {
		return fCode;
	}
	
	public String getMessage() {
		return fMessage;
	}
	
}
