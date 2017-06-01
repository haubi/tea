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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Service interface for TaskChains. A typical {@link TaskChain} will look like
 * this:
 *
 * <pre>
 * &#64;TaskChainId(description = "Do Something Useful", alias = { "Sample", "Test" })
 * &#64;TaskChainMenuEntry(path = { "My", "Menu Entry..." }, icon = "resources/sample.gif")
 * &#64;Component
 * public class SampleTaskChain implements TaskChain {
 *
 * 	&#64;TaskChainContextInit
 * 	public void initContext(TaskExecutionContext context) {
 * 		context.addTask(DemoTask.class); // either class to use DI
 * 		context.addTask(new DemoTask()); // or instance to configure manually
 * 	}
 * }
 * </pre>
 *
 * The most important parts in the code above are the
 * <code>&#64;Component</code> annotations, as well as the method annotated with
 * <code>&#64;TaskChainContextInit</code>, which is responsible for adding tasks
 * to the execution context.
 */
public interface TaskChain {

	/**
	 * Annotates a task chain for the TEA (Tasking Engine Advanced).
	 */
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface TaskChainId {
		/**
		 * @return the human readable name of the task chain
		 */
		String description();

		/**
		 * @return one or more alias for the task chain. These can be used to
		 *         reference the task chain to launch it from the command line
		 *         or IDE.
		 */
		String[] alias() default {};

		/**
		 * @return the number of retries that should be allows for the task
		 *         chain on failure.
		 */
		int retries() default 1;
	}

}
