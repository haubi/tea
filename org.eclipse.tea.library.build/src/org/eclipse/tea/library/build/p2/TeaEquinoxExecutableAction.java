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
package org.eclipse.tea.library.build.p2;

import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.eclipse.EquinoxExecutableAction;
import org.eclipse.equinox.p2.publisher.eclipse.IBrandingAdvice;

/**
 * Ensures that the branding can find the correct icon to be used
 */
@SuppressWarnings("restriction")
public class TeaEquinoxExecutableAction extends EquinoxExecutableAction {

	public TeaEquinoxExecutableAction(ExecutablesDescriptor executables, String configSpec, String idBase,
			Version version, String flavor) {
		super(executables, configSpec, idBase, version, flavor);
	}

	@Override
	protected void fullBrandExecutables(ExecutablesDescriptor descriptor, IBrandingAdvice advice) {
		super.fullBrandExecutables(descriptor, new TeaBrandingAdvice(advice));
	}
}
