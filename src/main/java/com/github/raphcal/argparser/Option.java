/*
 * Copyright (C) 2018 Raphaël Calabro <raph_kun at yahoo.fr>.
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
 * Describes an option (also called a flag).
 * 
 * @author Raphaël Calabro (ddaeke-github@yahoo.fr)
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Option {
	/**
	 * Alias to use instead of the property name for the option.
	 * <p>
	 * Useful to change an option name from camelCase to dash-case.
	 * 
	 * @return the alias of the option.
	 */
	String alias() default "";
	
	/**
	 * Description to print in the usage.
	 * 
	 * @return the description of the description.
	 */
	String description();
	
	/**
	 * Should be set to <code>true</code> if this option can be used without
	 * the required arguments (for example: help option or version option).
	 * 
	 * @return <code>true</code> if this option is autonomous, <code>false</code> otherwise.
	 */
	boolean autonomous() default false;
}
