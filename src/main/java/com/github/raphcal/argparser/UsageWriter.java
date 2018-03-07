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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Raphaël Calabro (ddaeke-github@yahoo.fr)
 */
public class UsageWriter extends Writer {
    
    private static final int DEFAULT_LINE_LENGTH = 80;
	private static final int PREFIX_LENGTH = 9;
    
    private int lineLength = DEFAULT_LINE_LENGTH;
    
    private final Writer innerWriter;

    public UsageWriter(Writer innerWriter) {
        this.innerWriter = innerWriter;
    }
    
    public UsageWriter(OutputStream outputStream) {
        this(new OutputStreamWriter(outputStream));
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        innerWriter.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        innerWriter.flush();
    }

    @Override
    public void close() throws IOException {
        innerWriter.close();
    }
    
    /**
	 * Write the usage of the given parser.
	 * 
     * @param <T> Argument type.
     * @param parser Argument parser instance.
	 * @param appName Name of the application.
	 * @throws IOException If an error occurs while printing the usage.
	 */
    public <T> void write(ArgumentParser<T> parser, String appName) throws IOException {
        final String lineSeparator = System.getProperty("line.separator");
		
		write("Usage: java -jar ");
		write(appName);
		
        final Map<String, ArgumentParserEntry> options = parser.getOptions();
        
		if(!options.isEmpty()) {
			write(" [options]");
		}
		
        final List<ArgumentParserEntry> arguments = parser.getArguments();
		final ArrayList<ArgumentParserEntry> enums = new ArrayList<ArgumentParserEntry>();
		
		for(final ArgumentParserEntry entry : arguments) {
			if (!entry.isOptional()) {
				write(" <");
				write(entry.getField().getName());
				write(">");
			} else {
				write(" [");
				write(entry.getField().getName());
				write("]");
			}
			
			if(entry.isCollection()) {
				write(" [...]");
			}
			if(entry.isEnumType()) {
				enums.add(entry);
			}
		}
		write(lineSeparator);
		
		if(!enums.isEmpty()) {
			for(final ArgumentParserEntry entry : enums) {
				write(capitalize(plurialize(entry.getField().getName())));
				write(lineSeparator);
				
				for (final Object constant : entry.getField().getType().getEnumConstants()) {
					final Enum<?> enumConstant = (Enum<?>)constant;
					write("  ");
					write(enumConstant.name().toLowerCase());
					write(lineSeparator);
				}
			}
		}
		
		if(!options.isEmpty()) {
			write("Options");
			write(lineSeparator);
            
            final int maxOptionNameLength = options.keySet().stream()
                    .map(option -> option.length())
                    .reduce(Math::max)
                    .orElse(0);
			
			final LinkedHashSet<ArgumentParserEntry> entries = new LinkedHashSet<ArgumentParserEntry>();
			for(final Map.Entry<String, ArgumentParserEntry> entry : options.entrySet()) {
				entries.add(entry.getValue());
			}
			for(final ArgumentParserEntry entry : entries) {
				final String name = entry.getAlias();
				
				write("  ");
				write(entry.getShortName());
				write(", --" + name);
				write(createSpace(maxOptionNameLength - name.length() + 1));
				
				final String[] lines = cleanCut(entry.getDescription(), maxOptionNameLength);
				for(final String line : lines) {
					write(line);
					write(lineSeparator);
				}
			}
		}
		
		flush();
    }
    
    /**
	 * Change the line length for the usage.
	 *
	 * @param lineLength Line length of the terminal window.
	 */
	public void setLineLength(int lineLength) {
		this.lineLength = lineLength;
	}
    
    private String createSpace(int length) {
		final char[] chars = new char[length];
		Arrays.fill(chars, ' ');
		return new String(chars);
	}
    
    private String[] cleanCut(String description, int maxOptionNameLength) {
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
    
}
