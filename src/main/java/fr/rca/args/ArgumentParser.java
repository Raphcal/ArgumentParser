/*
 * Copyright (C) 2013 Raphaël Calabro (ddaeke-github@yahoo.fr)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.rca.args;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
	private static final int DEFAULT_LINE_LENGTH = 80;
	private static final int PREFIX_LENGTH = 9;
	
	private final Class<T> clazz;
			
	private Map<String, ArgumentParserEntry> options;
	private List<ArgumentParserEntry> arguments;
	private int maxOptionNameLength;
	
	private int nonOptionalArgumentCount;
	
	private int lineLength = DEFAULT_LINE_LENGTH;
	
	/**
	 * Creates a new parser for the given argument list class.
	 *
	 * @param clazz Class containing field annotated with {@link Argument}
	 * and/or {@link Option} anotations.
	 */
	public ArgumentParser(Class<T> clazz) {
		this.clazz = clazz;
		
		this.options = new LinkedHashMap<String, ArgumentParserEntry>();
		this.arguments = new ArrayList<ArgumentParserEntry>();
		final Map<ArgumentParserEntry, Integer> priorities = new HashMap<ArgumentParserEntry, Integer>();
		
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
				
				if(name.length() > maxOptionNameLength) {
					maxOptionNameLength = name.length();
				}
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
		
		Collections.sort(arguments, new Comparator<ArgumentParserEntry>() {
			@Override
			public int compare(ArgumentParserEntry o1, ArgumentParserEntry o2) {
				return priorities.get(o1).compareTo(priorities.get(o2));
			}
		});
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

	/**
	 * Change the line length for the usage.
	 *
	 * @param lineLength Line length of the terminal window.
	 */
	public void setLineLength(int lineLength) {
		this.lineLength = lineLength;
	}
	
	/**
	 * Prints the usage to the given output stream.
	 * 
	 * @param appName Name of the application.
	 * @param outputStream Outputstream to use.
	 * @throws IOException If an error occurs while printing the usage.
	 */
	public void printUsage(String appName, OutputStream outputStream) throws IOException {
		printUsage(appName, new OutputStreamWriter(outputStream, Charset.defaultCharset()));
	}
	
	/**
	 * Prints the usage to the given writer.
	 * 
	 * @param appName Name of the application.
	 * @param writer Writer to use.
	 * @throws IOException If an error occurs while printing the usage.
	 */
	public void printUsage(String appName, Writer writer) throws IOException {
		final String lineSeparator = System.getProperty("line.separator");
		
		writer.write("Usage: java -jar ");
		writer.write(appName);
		
		if(!options.isEmpty()) {
			writer.write(" [options]");
		}
		
		final ArrayList<ArgumentParserEntry> enums = new ArrayList<ArgumentParserEntry>();
		
		for(final ArgumentParserEntry entry : arguments) {
			if (!entry.isOptional()) {
				writer.write(" <");
				writer.write(entry.getField().getName());
				writer.write(">");
			} else {
				writer.write(" [");
				writer.write(entry.getField().getName());
				writer.write("]");
			}
			
			if(entry.isCollection()) {
				writer.write(" [...]");
			}
			if(entry.isEnumType()) {
				enums.add(entry);
			}
		}
		writer.write(lineSeparator);
		
		if(!enums.isEmpty()) {
			for(final ArgumentParserEntry entry : enums) {
				writer.write(capitalize(plurialize(entry.getField().getName())));
				writer.write(lineSeparator);
				
				for (final Object constant : entry.getField().getType().getEnumConstants()) {
					final Enum<?> enumConstant = (Enum<?>)constant;
					writer.write("  ");
					writer.write(enumConstant.name().toLowerCase());
					writer.write(lineSeparator);
				}
			}
		}
		
		if(!options.isEmpty()) {
			writer.write("Options");
			writer.write(lineSeparator);
			
			final LinkedHashSet<ArgumentParserEntry> entries = new LinkedHashSet<ArgumentParserEntry>();
			for(final Map.Entry<String, ArgumentParserEntry> entry : options.entrySet()) {
				entries.add(entry.getValue());
			}
			for(final ArgumentParserEntry entry : entries) {
				final String name = entry.getAlias();
				
				writer.write("  ");
				writer.write(entry.getShortName());
				writer.write(", --" + name);
				writer.write(createSpace(maxOptionNameLength - name.length() + 1));
				
				final String[] lines = cleanCut(entry.getDescription());
				for(final String line : lines) {
					writer.write(line);
					writer.write(lineSeparator);
				}
			}
		}
		
		writer.flush();
	}
	
	private <I> I newInstance(Class<I> clazz) {
		try {
			return clazz.newInstance();
		} catch (InstantiationException ex) {
			Logger.getLogger(ArgumentParser.class.getName()).log(Level.SEVERE, "Impossible d'instancier '" + clazz +"'. Chargement des arguments impossible.", ex);
		} catch (IllegalAccessException ex) {
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
			
		} catch (SecurityException ex) {
			Logger.getLogger(ArgumentParser.class.getName()).log(Level.SEVERE, "Accès non autorisé au champ '" + entry.getField().getName() + "'.", ex);
		} catch (IllegalArgumentException ex) {
			Logger.getLogger(ArgumentParser.class.getName()).log(Level.SEVERE, "Le champ '" + entry.getField().getName() + "' n'est pas compatible avec le type '" + value.getClass().getName() + "'.", ex);
		} catch (IllegalAccessException ex) {
			Logger.getLogger(ArgumentParser.class.getName()).log(Level.SEVERE, "Accès non autorisé au champ '" + entry.getField().getName() + "'.", ex);
		}
	}
	
	private String createSpace(int length) {
		final char[] chars = new char[length];
		Arrays.fill(chars, ' ');
		return new String(chars);
	}
	
	private String[] cleanCut(String description) {
		final ArrayList<String> lines = new ArrayList<String>();
		
		final StringBuilder stringBuilder = new StringBuilder(description);
		final int maxLength = lineLength - PREFIX_LENGTH - maxOptionNameLength;
		
		while(stringBuilder.length() > maxLength) {
			int cutIndex = stringBuilder.lastIndexOf(" ", maxLength);
			if(cutIndex == -1) {
				cutIndex = maxLength;
			}
			lines.add((lines.isEmpty() ? "" : createSpace(PREFIX_LENGTH + maxOptionNameLength)) + 
					stringBuilder.substring(0, cutIndex));
			stringBuilder.replace(0, cutIndex + 1, "");
		}
		lines.add((lines.isEmpty() ? "" : createSpace(PREFIX_LENGTH + maxOptionNameLength)) + 
					stringBuilder.toString());
		
		return lines.toArray(new String[0]);
	}
	
	private String capitalize(final String source) {
		return Character.toUpperCase(source.charAt(0)) + source.substring(1);
	}
	
	private String plurialize(final String source) {
		if (source.endsWith("y")) {
			return source.substring(0, source.length() - 2) + "ies";
		} else {
			return source + 's';
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
