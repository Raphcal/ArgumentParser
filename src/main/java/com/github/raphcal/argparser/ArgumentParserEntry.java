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

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Entry of the argument parser.
 * 
 * @author Raphaël Calabro (ddaeke-github@yahoo.fr)
 */
class ArgumentParserEntry {
	
	private final Field field;
	private final boolean hasValue;
	private final boolean collection;
	private final boolean enumType;
	private boolean autonomous;
	private String alias;
	private String shortName;
	private String description;
	private boolean optional;

	public ArgumentParserEntry(Field field, boolean hasValue) {
		this.field = field;
		this.hasValue = hasValue;
		this.collection = Collection.class.isAssignableFrom(field.getType());
		this.enumType = field.getType().isEnum();
	}

	public Field getField() {
		return field;
	}

	public boolean hasValue() {
		return hasValue;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setAutonomous(boolean autonomous) {
		this.autonomous = autonomous;
	}

	public boolean isAutonomous() {
		return autonomous;
	}

	public boolean isCollection() {
		return collection;
	}

	public boolean isEnumType() {
		return enumType;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public boolean isOptional() {
		return optional;
	}
	
}
