/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.mapping;

import com.google.common.base.Charsets;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MappingsEnigmaWriter {

	public void write(File out, Mappings mappings, boolean isDirectoryFormat) throws IOException {
		if (!isDirectoryFormat) {
			PrintWriter outputWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), Charsets.UTF_8));
			write(outputWriter, mappings);
			outputWriter.close();
		} else
			writeAsDirectory(out, mappings);
	}

	public void writeAsDirectory(File target, Mappings mappings) throws IOException {
		if (!target.exists() && !target.mkdirs())
			throw new IOException("Cannot create mapping directory!");

		for (ClassMapping classMapping : sorted(mappings.classes())) {
			if (!classMapping.isDirty())
				continue;
			this.deletePreviousClassMapping(target, classMapping);
			File obFile = new File(target, classMapping.getObfFullName() + ".mapping");
			File result;
			if (classMapping.getDeobfName() == null)
				result = obFile;
			else {
				// Make sure that old version of the file doesn't exist
				if (obFile.exists())
					obFile.delete();
				result = new File(target, classMapping.getDeobfName() + ".mapping");
			}

			if (!result.getParentFile().exists())
				result.getParentFile().mkdirs();
			result.createNewFile();
			PrintWriter outputWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(result), Charsets.UTF_8));
			write(outputWriter, classMapping, 0);
			outputWriter.close();
		}

		// Remove dropped mappings
		if (mappings.getPreviousState() != null) {
			List<ClassMapping> droppedClassMappings = new ArrayList<>(mappings.getPreviousState().classes());
			List<ClassMapping> classMappings = new ArrayList<>(mappings.classes());
			droppedClassMappings.removeAll(classMappings);
			for (ClassMapping classMapping : droppedClassMappings) {
				File obFile = new File(target, classMapping.getObfFullName() + ".mapping");
				File result;
				if (classMapping.getDeobfName() == null)
					result = obFile;
				else {
					// Make sure that old version of the file doesn't exist
					if (obFile.exists())
						obFile.delete();
					result = new File(target, classMapping.getDeobfName() + ".mapping");
				}
				if (result.exists())
					result.delete();
			}
		}
	}

	private void deletePreviousClassMapping(File target, ClassMapping classMapping) {
		File prevFile = null;
		// Deob rename
		if (classMapping.getDeobfName() != null && classMapping.getPreviousDeobfName() != null && !classMapping.getPreviousDeobfName().equals(classMapping.getDeobfName())) {
			prevFile = new File(target, classMapping.getPreviousDeobfName() + ".mapping");
		}
		// Deob to ob rename
		else if (classMapping.getDeobfName() == null && classMapping.getPreviousDeobfName() != null) {
			prevFile = new File(target, classMapping.getPreviousDeobfName() + ".mapping");
		}
		// Ob to Deob rename
		else if (classMapping.getDeobfName() != null && classMapping.getPreviousDeobfName() == null) {
			prevFile = new File(target, classMapping.getObfFullName() + ".mapping");
		}

		if (prevFile != null && prevFile.exists())
			prevFile.delete();
	}

	public void write(PrintWriter out, Mappings mappings) throws IOException {
		for (ClassMapping classMapping : sorted(mappings.classes())) {
			write(out, classMapping, 0);
		}
	}

	private void write(PrintWriter out, ClassMapping classMapping, int depth) throws IOException {
		if (classMapping.getDeobfName() == null) {
			out.format("%sCLASS %s%s\n", getIndent(depth), classMapping.getObfFullName(),
				classMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : classMapping.getModifier().getFormattedName());
		} else {
			out.format("%sCLASS %s %s%s\n", getIndent(depth), classMapping.getObfFullName(), classMapping.getDeobfName(),
				classMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : classMapping.getModifier().getFormattedName());
		}

		for (ClassMapping innerClassMapping : sorted(classMapping.innerClasses())) {
			write(out, innerClassMapping, depth + 1);
		}

		for (FieldMapping fieldMapping : sorted(classMapping.fields())) {
			write(out, fieldMapping, depth + 1);
		}

		for (MethodMapping methodMapping : sorted(classMapping.methods())) {
			write(out, methodMapping, depth + 1);
		}
	}

	private void write(PrintWriter out, FieldMapping fieldMapping, int depth) {
		if (fieldMapping.getDeobfName() == null)
			out.format("%sFIELD %s %s%s\n", getIndent(depth), fieldMapping.getObfName(), fieldMapping.getObfDesc().toString(),
				fieldMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : fieldMapping.getModifier().getFormattedName());
		else
			out.format("%sFIELD %s %s %s%s\n", getIndent(depth), fieldMapping.getObfName(), fieldMapping.getDeobfName(), fieldMapping.getObfDesc().toString(),
				fieldMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : fieldMapping.getModifier().getFormattedName());
	}

	private void write(PrintWriter out, MethodMapping methodMapping, int depth) throws IOException {
		if (methodMapping.isObfuscated()) {
			out.format("%sMETHOD %s %s%s\n", getIndent(depth), methodMapping.getObfName(), methodMapping.getObfDesc(),
				methodMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : methodMapping.getModifier().getFormattedName());
		} else {
			out.format("%sMETHOD %s %s %s%s\n", getIndent(depth), methodMapping.getObfName(), methodMapping.getDeobfName(), methodMapping.getObfDesc(),
				methodMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : methodMapping.getModifier().getFormattedName());
		}

		for (LocalVariableMapping localVariableMapping : sorted(methodMapping.arguments())) {
			write(out, localVariableMapping, depth + 1);
		}
	}

	private void write(PrintWriter out, LocalVariableMapping localVariableMapping, int depth) {
		out.format("%sARG %d %s\n", getIndent(depth), localVariableMapping.getIndex(), localVariableMapping.getName());
	}

	private <T extends Comparable<T>> List<T> sorted(Iterable<T> classes) {
		List<T> out = new ArrayList<>();
		for (T t : classes) {
			out.add(t);
		}
		Collections.sort(out);
		return out;
	}

	private String getIndent(int depth) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			buf.append("\t");
		}
		return buf.toString();
	}
}
