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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Permet d'analyser les arguments donnés à un programme et d'afficher
 * la liste des arguments attendus.
 * 
 * @author Raphaël Calabro (ddaeke-github@yahoo.fr)
 * @param <T> Type décrivant les arguments du programme.
 */
public class ArgumentParser<T> {
	private final Class<T> clazz;
			
	private final Map<String, ArgumentParserEntry> options;
	private final List<ArgumentParserEntry> arguments;
	
	private int nonOptionalArgumentCount;
	
	/**
	 * Creates a new parser for the given argument list class.
	 *
	 * @param clazz Class containing field annotated with {@link Argument}
	 * and/or {@link Option} anotations.
	 */
	public ArgumentParser(Class<T> clazz) {
		this.clazz = clazz;
		
		this.options = new LinkedHashMap<>();
		this.arguments = new ArrayList<>();
		final Map<ArgumentParserEntry, Integer> priorities = new HashMap<>();
		
		final Field[] fields = clazz.getDeclaredFields();
		for(final Field field : fields) {
			final ArgumentParserEntry entry = new ArgumentParserEntry(field, field.getType() != boolean.class);
			
			final Option option = field.getAnnotation(Option.class);
			if(option != null) {
				field.setAccessible(true);
				
				final String name = option.alias().isEmpty() ? field.getName() : option.alias();
				String shortName = "-" + name.charAt(0);
				if(options.containsKey(shortName)) {
					shortName = "-" + Character.toUpperCase(name.charAt(0));
				}
				if(options.containsKey(shortName)) {
					throw new IllegalArgumentException("Les options -" + name.charAt(0) + " et -" + 
							Character.toUpperCase(name.charAt(0)) + " sont déjà utilisés, choisissez un alias différent pour l'option '" + name +"'.");
				}
				
				options.put(shortName, entry);
				options.put("--" + name, entry);
				
				entry.setAlias(name);
				entry.setShortName(shortName);
				entry.setDescription(option.description());
				entry.setAutonomous(option.autonomous());
			}
			
			final Argument argument = field.getAnnotation(Argument.class);
			if(argument != null) {
				field.setAccessible(true);
				arguments.add(entry);
				priorities.put(entry, argument.index());
				
				entry.setOptional(argument.optional());
				if (!argument.optional()) {
					nonOptionalArgumentCount++;
				}
			}
		}
		
        arguments.sort((o1, o2) -> priorities.get(o1).compareTo(priorities.get(o2)));
	}
	
	/**
	 * Parse the given program arguments and creates an instance of
	 * the argument list class.
	 * <p>
	 * If the number of required argument is not met, this method returns
	 * <code>null</code>.
	 * 
	 * @param args Program arguments.
	 * @return A new instance of <code>T</code> or <code>null</code> if the
	 * parsing can't be completed.
	 */
	public T parse(String[] args) {
		final T t = newInstance(clazz);
		
		int currentArgument = 0;
		boolean valid = true;
		boolean forceValid = false;
		boolean collection = false;
		
		for(int index = 0; index < args.length; index++) {
			final String arg = args[index];
			
			ArgumentParserEntry entry = options.get(arg);
			if(entry != null) {
				if(entry.hasValue()) {
					index++;
					if(index < args.length) {
						set(t, entry, arg);
					} else {
						valid = false;
					}
					
					if(entry.isCollection()) {
						while(index < args.length) {
							if(options.get(args[index]) != null) {
								index--;
								break;
							}
							set(t, entry, args[index++]);
						}
					}
					
				} else {
					set(t, entry, true);
				}
				
				forceValid |= entry.isAutonomous();
				
			} else if(currentArgument < arguments.size()) {
				entry = arguments.get(currentArgument);
				
				final Object value;
				if (entry.isEnumType()) {
					value = toEnumValue(entry, arg);
					if (value == null) {
						return null;
					}
				} else {
					value = arg;
				}
				
				set(t, entry, value);
				
				if(!entry.isCollection()) {
					currentArgument++;
				} else {
					collection = true;
				}
				
			} else {
				valid = false;
			}
		}
		
		if (collection) {
			currentArgument++;
		}
		
		final boolean validArgumentCount = currentArgument >= nonOptionalArgumentCount && currentArgument <= arguments.size();
		
		if(forceValid || (valid && validArgumentCount)) {
			return t;
		} else {
			return null;
		}
	}

    List<ArgumentParserEntry> getArguments() {
        return arguments;
    }

    Map<String, ArgumentParserEntry> getOptions() {
        return options;
    }

	/**
	 * Prints the usage to the given writer.
	 * 
	 * @param appName Name of the application.
	 * @param writer Writer to use.
	 * @throws IOException If an error occurs while printing the usage.
	 */
	
	private <I> I newInstance(Class<I> clazz) {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
			Logger.getLogger(ArgumentParser.class.getName()).log(Level.SEVERE, "Impossible d'instancier '" + clazz +"'. Chargement des arguments impossible.", ex);
		}
		return null;
	}
	
	private <V> void set(T t, ArgumentParserEntry entry, V value) {
		try {
			if(!entry.isCollection()) {
				entry.getField().set(t, value);
			} else {
				Object object = entry.getField().get(t);
				
				if(object == null) {
					object = newInstance(entry.getField().getType());
					entry.getField().set(t, object);
				}
				
				if (object instanceof Collection) {
					final Collection<V> collection = (Collection<V>) object;
					collection.add(value);
				}
			}
			
		} catch (SecurityException | IllegalAccessException ex) {
			Logger.getLogger(ArgumentParser.class.getName()).log(Level.SEVERE, "Accès non autorisé au champ '" + entry.getField().getName() + "'.", ex);
		} catch (IllegalArgumentException ex) {
			Logger.getLogger(ArgumentParser.class.getName()).log(Level.SEVERE, "Le champ '" + entry.getField().getName() + "' n'est pas compatible avec le type '" + value.getClass().getName() + "'.", ex);
		}
	}
	
	private <T extends Enum<T>> T toEnumValue(ArgumentParserEntry entry, String value) {
		@SuppressWarnings("unchecked")
		final Class<T> enumType = (Class<T>)entry.getField().getType();
		try {
			return Enum.valueOf(enumType, value.toUpperCase());
		} catch (IllegalArgumentException e) {
			// Ignored.
		}
		return null;
	} 
	
}
