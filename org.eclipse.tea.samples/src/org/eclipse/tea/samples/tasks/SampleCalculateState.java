/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.samples.tasks;

import org.eclipse.e4.core.di.annotations.Execute;

public class SampleCalculateState {

	public static String myState = null;

	@Execute
	public void run() {
		myState = "This is some calculated state";
	}

}
