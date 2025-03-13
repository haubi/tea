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
package org.eclipse.tea.core.ui;

import java.io.PrintStream;

import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tea.core.internal.TimeHelper;
import org.eclipse.tea.core.internal.config.CoreConfig;
import org.eclipse.tea.core.internal.config.TaskingDevelopmentConfig;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.core.services.TaskingLog.TaskingLogQualifier;
import org.eclipse.tea.core.ui.config.TaskingConsoleConfig;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.service.component.annotations.Component;

/**
 * Console access for WAMAS IDE plug-ins. The console offers 4 streams in
 * different colors for printing messages.
 * <p>
 * If this class is used outside the IDE, all output goes to {@code System.out}
 * or {@code System.err}.
 */
@SuppressWarnings("restriction")
@Component(service = TaskingLog.class)
@TaskingLogQualifier(headless = false)
public class TaskingConsole extends TaskingLog {

	private static final String TASKING_CONSOLE = "Tasking Console";
	private static final ImageDescriptor TEA_ICON = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
			"resources/tea.png");

	public PrintStream outInfo;
	public PrintStream outStd;
	public PrintStream outWrn;
	public PrintStream outErr;

	private volatile MessageConsole myConsole;
	private volatile MessageConsoleStream sInfo, sStd, sWrn, sErr;

	private CoreConfig cfg;
	private volatile TaskingConsoleConfig config;

	@TaskingLogInit
	public void init(TaskingConsoleConfig config, TaskingDevelopmentConfig devConfig, CoreConfig coreCfg) {
		this.cfg = coreCfg;
		this.config = config;

		setShowDebug(devConfig.showDebugLogs);
		if (myConsole == null) {
			myConsole = findConsole(TASKING_CONSOLE);
			sInfo = myConsole.newMessageStream();
			sStd = myConsole.newMessageStream();
			sWrn = myConsole.newMessageStream();
			sErr = myConsole.newMessageStream();

			outInfo = new PrintStream(sInfo);
			outStd = new PrintStream(sStd);
			outWrn = new PrintStream(sWrn);
			outErr = new PrintStream(sErr);

			myConsole.activate();
		}
		Display.getDefault().asyncExec(this::updateConfig);
	}

	@Override
	public void bringToFront() {
		if (myConsole != null) {
			myConsole.activate();
		}
	}

	void updateConfig() {
		if (cfg.useAccessibleMode) {
			myConsole.setConsoleWidth(80);
		} else {
			myConsole.setConsoleWidth(0);
		}
		if (config.useColors) {
			boolean isDarkTheme = ((IThemeEngine) Display.getDefault().getData("org.eclipse.e4.ui.css.swt.theme"))
					.getActiveTheme().getLabel().equals("Dark");

			if (config.useDarkColors || isDarkTheme) {
				sInfo.setColor(new Color(null, 164, 164, 164));
				sStd.setColor(new Color(null, 255, 255, 255));
				sWrn.setColor(new Color(null, 255, 190, 50));
				sErr.setColor(new Color(null, 255, 100, 100));
			} else {
				sInfo.setColor(new Color(null, 164, 164, 164));
				sStd.setColor(new Color(null, 32, 32, 32));
				sWrn.setColor(new Color(null, 255, 190, 0));
				sErr.setColor(new Color(null, 255, 0, 0));
			}
		} else {
			sInfo.setColor(null);
			sStd.setColor(null);
			sWrn.setColor(null);
			sErr.setColor(null);
		}
	}

	/**
	 * Flushes all output streams.
	 */
	public void flush() {
		outInfo.flush();
		outStd.flush();
		outWrn.flush();
		outErr.flush();
	}

	@Deprecated
	protected void finalize() throws Throwable {
		close(sInfo);
		close(sStd);
		close(sWrn);
		close(sErr);
		super.finalize();
	}

	private static void close(MessageConsoleStream s) {
		try {
			if (s != null && !s.isClosed()) {
				s.close();
			}
		} catch (Exception e) {
			// ignore it
		}
	}

	/**
	 * Find a console for writing out messages. Reuse an existing one or create
	 * a new one.
	 *
	 * @param name
	 *            name of the console
	 * @return a MessageConsole; null if we are outside the IDE
	 */
	private static MessageConsole findConsole(String name) {
		try {
			ConsolePlugin plugin = ConsolePlugin.getDefault();
			IConsoleManager conMan = plugin.getConsoleManager();
			IConsole[] existing = conMan.getConsoles();
			for (IConsole ic : existing) {
				if (name.equals(ic.getName())) {
					return (MessageConsole) ic;
				}
			}

			// no console found, so create a new one
			MessageConsole myConsole = new MessageConsole(name, TEA_ICON);

			// test the console
			MessageConsoleStream stream = myConsole.newMessageStream();
			try {
				conMan.addConsoles(new IConsole[] { myConsole });
			} finally {
				if (!stream.isClosed()) {
					stream.close();
				}
			}

			return myConsole;
		} catch (Exception ex) {
			throw new RuntimeException("cannot create console", ex);
		}
	}

	@Override
	public PrintStream debug() {
		return outInfo;
	}

	@Override
	public PrintStream info() {
		return outStd;
	}

	@Override
	public PrintStream warn() {
		return outWrn;
	}

	@Override
	public PrintStream error() {
		return outErr;
	}

	@Override
	public String formatMessage(String msg) {
		if (cfg == null || !cfg.useAccessibleMode) {
			return super.formatMessage(msg);
		}

		return TimeHelper.getFormattedCurrentTime() + "\n" + msg;
	}
}
