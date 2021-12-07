/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.core.services;

import java.io.PrintStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.eclipse.tea.core.internal.TimeHelper;

/**
 * Very simple logging "facade". Implementations are either std output streams
 * or the tasking console.
 */
public abstract class TaskingLog {

	/**
	 * Required annotation on TaskingLog services to be able to choose a proper
	 * one.
	 */
	@Documented
	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface TaskingLogQualifier {
		boolean headless();
	}

	/**
	 * Denotes an init method in a {@link TaskingLog} service that should be
	 * called when the according service is chosen.
	 */
	@Documented
	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface TaskingLogInit {

	}

	private boolean showDebug = true;

	protected void setShowDebug(boolean dbg) {
		showDebug = dbg;
	}

	/**
	 * @return the stream used for info output.
	 */
	public abstract PrintStream debug();

	/**
	 * @return the stream used for standard log output.
	 */
	public abstract PrintStream info();

	/**
	 * @return the stream used for warnings.
	 */
	public abstract PrintStream warn();

	/**
	 * @return the stream used for errors and exceptions.
	 */
	public abstract PrintStream error();

	/**
	 * simple default log to the standard log output destination.
	 */
	public void info(String msg) {
		info(msg, (Throwable) null);
	}

	/**
	 * simple log including exception stack trace to the standard log output.
	 */
	public void info(String msg, Throwable t) {
		write(info(), msg, t);
	}

	/**
	 * formatted log output (using {@link String#format(String, Object...)}) to
	 * the standard log output.
	 */
	public void info(String msg, Object... args) {
		info(String.format(msg, args));
	}

	/**
	 * @see #info(String)
	 */
	public void debug(String msg) {
		if (showDebug) {
			debug(msg, (Throwable) null);
		}
	}

	/**
	 * @see #info(String, Throwable)
	 */
	public void debug(String msg, Throwable t) {
		if (showDebug) {
			write(debug(), msg, t);
		}
	}

	/**
	 * @see #info(String, Object...)
	 */
	public void debug(String msg, Object... args) {
		if (showDebug) {
			debug(String.format(msg, args));
		}
	}

	/**
	 * @see #info(String)
	 */
	public void warn(String msg) {
		warn(msg, (Throwable) null);
	}

	/**
	 * @see #info(String, Throwable)
	 */
	public void warn(String msg, Throwable t) {
		write(warn(), msg, t);
	}

	/**
	 * @see #info(String, Object...)
	 */
	public void warn(String msg, Object... args) {
		warn(String.format(msg, args));
	}

	/**
	 * @see #info(String)
	 */
	public void error(String msg) {
		error(msg, (Throwable) null);
	}

	/**
	 * @see #info(String, Throwable)
	 */
	public void error(String msg, Throwable t) {
		write(error(), msg, t);
	}

	/**
	 * @see #info(String, Object...)
	 */
	public void error(String msg, Object... args) {
		error(String.format(msg, args));
	}

	/**
	 * Write the given message in a formatted way to the given stream, adding a
	 * stack trace for the given {@link Throwable} if not <code>null</code>.
	 */
	public void write(PrintStream s, String msg, Throwable t) {
		s.println(formatMessage(msg));
		if (t != null) {
			t.printStackTrace(s);
		}
	}

	/**
	 * Format the given message in a way suitable to write to the log (i.e. add
	 * timestamp, etc.).
	 */
	public String formatMessage(String msg) {
		return "[TEA " + TimeHelper.getFormattedCurrentTime() + "] " + msg;
	}

	/**
	 * If the Log is a Console Window the Window will be brought to front;
	 */
	public void bringToFront() {
		//
	}

}
