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
package org.eclipse.tea.core.internal;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.Service;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.tea.core.TaskingEngine;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.internal.config.PropertyConfigurationStore;
import org.eclipse.tea.core.services.TaskingHeadlessLifeCycle;
import org.eclipse.tea.core.services.TaskingHeadlessLifeCycle.HeadlessShutdown;
import org.eclipse.tea.core.services.TaskingHeadlessLifeCycle.HeadlessStartup;
import org.eclipse.tea.core.services.TaskingHeadlessLifeCycle.StartupAction;
import org.eclipse.tea.core.services.TaskingLog;

import com.google.common.base.Strings;

/**
 * Root entry point for headless task execution. Handles evaluation of the
 * command line as well as managing the "headless application lifecycle" (see
 * {@link TaskingHeadlessLifeCycle}).
 */
public class TaskingEngineApplication implements IApplication {

	public static final Integer EXIT_FAILED = new Integer(1);
	public static final Integer EXIT_EXCEPTION = new Integer(2);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		String argPropertiesFile = null;
		String argTaskChain = null;

		String args[] = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		for (int i = 0; i < args.length; ++i) {
			String arg = args[i];
			if ("-properties".equals(arg) || "-configuration".equals(arg)) {
				argPropertiesFile = args[i + 1];
			}
			if ("-taskchain".equals(arg) || "-execute".equals(arg)) {
				argTaskChain = args[i + 1];
			}
		}

		File propFile = null;
		if (argPropertiesFile != null) {
			propFile = new File(argPropertiesFile);
			if (!propFile.exists()) {
				throw new RuntimeException("configuration file does not exist: " + propFile);
			}
		}

		IEclipseContext rootContext = TaskingInjectionHelper.getRootContext();

		// mark headless application
		rootContext.set(TaskingInjectionHelper.CTX_HEADLESS, true);

		// create engine with configuration from properties
		TaskingEngine engine = TaskingEngine.withConfiguration(getPreferenceStore(propFile));

		// query startup contributions (auto-update, etc.). Use the configured
		// context to allow contributions to query configuration values already.
		LifeCycleHelper helper = new LifeCycleHelper();
		boolean restart = (boolean) ContextInjectionFactory.invoke(helper, HeadlessStartup.class, engine.getContext());
		if (restart) {
			return EXIT_RESTART;
		}

		try {
			if (Strings.isNullOrEmpty(argTaskChain)) {
				throw new RuntimeException("Nothing to do. Use the -execute argument.");
			} else {
				IStatus status = engine
						.runTaskChain(TaskingInjectionHelper.createNewChainContext(engine, argTaskChain, null));
				if (status.getSeverity() >= IStatus.ERROR) {
					return EXIT_FAILED;
				}
			}
		} catch (Exception e) {
			engine.getContext().get(TaskingLog.class).debug("Failed to run tasking", e);
			return EXIT_EXCEPTION;
		} finally {
			// notify interested contributions about application shutdown
			ContextInjectionFactory.invoke(helper, HeadlessShutdown.class, engine.getContext());
		}

		return EXIT_OK;
	}

	/**
	 * Creates a {@link TaskingConfigurationStore} that is capable of reading
	 * the given file (an may read other sources too).
	 *
	 * @param propFile
	 *            the property file to take into account
	 * @return a configuration store that reads from the given file (but may
	 *         read from more sources).
	 * @throws IOException
	 *             in case there was a problem with the given file.
	 */
	protected TaskingConfigurationStore getPreferenceStore(File propFile) throws IOException {
		return new PropertyConfigurationStore(propFile);
	}

	@Override
	public void stop() {
	}

	/**
	 * Helper that invokes all headless lifecycle contributions available in the
	 * system.
	 */
	private static final class LifeCycleHelper {

		@HeadlessStartup
		public boolean checkStartup(TaskingLog log, IEclipseContext context,
				@Service List<TaskingHeadlessLifeCycle> contributions) {
			for (TaskingHeadlessLifeCycle contrib : new TreeSet<>(contributions)) {
				try {
					StartupAction action = (StartupAction) ContextInjectionFactory.invoke(contrib,
							HeadlessStartup.class, context, StartupAction.CONTINUE);
					if (action == StartupAction.RESTART) {
						return true;
					}
				} catch (Exception e) {
					log.error("Failure in contribution @HeadlessStartup " + contrib.getClass().getName(), e);
					// prevent startup
					throw e;
				}
			}
			return false;
		}

		@HeadlessShutdown
		public void checkShutdown(TaskingLog log, IEclipseContext context,
				@Service List<TaskingHeadlessLifeCycle> contributions) {
			for (TaskingHeadlessLifeCycle contrib : new TreeSet<>(contributions)) {
				try {
					ContextInjectionFactory.invoke(contrib, HeadlessShutdown.class, context, null);
				} catch (Exception e) {
					log.error("Failure in contribution @HeadlessShutdown " + contrib.getClass().getName(), e);
				}
			}
			log.debug("Shutdown contributions done.");
		}

	}

}
