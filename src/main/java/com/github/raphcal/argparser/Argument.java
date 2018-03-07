/*
 * Copyright (C) 2018 Raphaël Calabro <ddaeke-github at yahoo.fr>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.github.raphcal.argparser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describes one of the argument.
 * @author Raphaël Calabro (ddaeke-github@yahoo.fr)
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Argument {
	/**
	 * Index of the argument. Every argument should have a different index.
	 * 
	 * @return the index of the argument.
	 */
	int index();

	/**
	 * Should be set to <code>true</code> if the argument is optional.
	 * 
	 * @return <code>true</code> if the argument is optional,
	 * <code>false</code> otherwise.
	 */
	boolean optional() default false;
}
