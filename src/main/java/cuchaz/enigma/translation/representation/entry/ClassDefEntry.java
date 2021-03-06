/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.translation.representation.entry;

import com.google.common.base.Preconditions;
import com.strobel.assembler.metadata.TypeDefinition;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.Signature;

import javax.annotation.Nullable;
import java.util.Arrays;

public class ClassDefEntry extends ClassEntry implements DefEntry<ClassEntry> {
	private final AccessFlags access;
	private final Signature signature;
	private final ClassEntry superClass;
	private final ClassEntry[] interfaces;

	public ClassDefEntry(String className, Signature signature, AccessFlags access, @Nullable ClassEntry superClass, ClassEntry[] interfaces) {
		this(getOuterClass(className), getInnerName(className), signature, access, superClass, interfaces);
	}

	public ClassDefEntry(ClassEntry parent, String className, Signature signature, AccessFlags access, @Nullable ClassEntry superClass, ClassEntry[] interfaces) {
		super(parent, className);
		Preconditions.checkNotNull(signature, "Class signature cannot be null");
		Preconditions.checkNotNull(access, "Class access cannot be null");

		this.signature = signature;
		this.access = access;
		this.superClass = superClass;
		this.interfaces = interfaces != null ? interfaces : new ClassEntry[0];
	}

	public static ClassDefEntry parse(int access, String name, String signature, String superName, String[] interfaces) {
		ClassEntry superClass = superName != null ? new ClassEntry(superName) : null;
		ClassEntry[] interfaceClasses = Arrays.stream(interfaces).map(ClassEntry::new).toArray(ClassEntry[]::new);
		return new ClassDefEntry(name, Signature.createSignature(signature), new AccessFlags(access), superClass, interfaceClasses);
	}

	public static ClassDefEntry parse(TypeDefinition def) {
		String name = def.getInternalName();
		Signature signature = Signature.createSignature(def.getSignature());
		AccessFlags access = new AccessFlags(def.getModifiers());
		ClassEntry superClass = def.getBaseType() != null ? ClassEntry.parse(def.getBaseType()) : null;
		ClassEntry[] interfaces = def.getExplicitInterfaces().stream().map(ClassEntry::parse).toArray(ClassEntry[]::new);
		return new ClassDefEntry(name, signature, access, superClass, interfaces);
	}

	public Signature getSignature() {
		return signature;
	}

	@Override
	public AccessFlags getAccess() {
		return access;
	}

	@Nullable
	public ClassEntry getSuperClass() {
		return superClass;
	}

	public ClassEntry[] getInterfaces() {
		return interfaces;
	}

	@Override
	public ClassDefEntry translate(Translator translator, @Nullable EntryMapping mapping) {
		Signature translatedSignature = translator.translate(signature);
		String translatedName = mapping != null ? mapping.getTargetName() : name;
		AccessFlags translatedAccess = mapping != null ? mapping.getAccessModifier().transform(access) : access;
		ClassEntry translatedSuper = translator.translate(superClass);
		ClassEntry[] translatedInterfaces = Arrays.stream(interfaces).map(translator::translate).toArray(ClassEntry[]::new);
		return new ClassDefEntry(parent, translatedName, translatedSignature, translatedAccess, translatedSuper, translatedInterfaces);
	}

	@Override
	public ClassDefEntry withName(String name) {
		return new ClassDefEntry(parent, name, signature, access, superClass, interfaces);
	}

	@Override
	public ClassDefEntry withParent(ClassEntry parent) {
		return new ClassDefEntry(parent, name, signature, access, superClass, interfaces);
	}
}
