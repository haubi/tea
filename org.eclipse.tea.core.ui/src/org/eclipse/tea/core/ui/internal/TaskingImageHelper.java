/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.core.ui.internal;

import org.eclipse.swt.graphics.Image;
import org.eclipse.tea.core.ui.Activator;

import com.google.common.base.Strings;

public class TaskingImageHelper {

	public static String getIconUri(String bundle, String icon) {
		if (Strings.isNullOrEmpty(icon)) {
			return null;
		}

		return "platform:plugin/" + bundle + "/" + icon;
	}

	public static Image getSharedIcon(String bundle, String icon) {
		if (Strings.isNullOrEmpty(icon)) {
			return null;
		}

		return Activator.imageDescriptorFromPlugin(bundle, icon).createImage();
	}

}
