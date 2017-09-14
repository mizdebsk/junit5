/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.jpms;

import java.io.IOException;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.platform.commons.JUnitException;

/**
 * JPMS module reference scanner.
 *
 * @since 1.1
 */
class ModuleReferenceScanner {

	private final Predicate<Class<?>> classTester;
	private final Predicate<String> classNameFilter;

	ModuleReferenceScanner(Predicate<Class<?>> classTester, Predicate<String> classNameFilter) {
		this.classTester = classTester;
		this.classNameFilter = classNameFilter;
	}

	/**
	 * Scan module reference for classes that potentially contain testable methods.
	 */
	List<Class<?>> scan(ModuleReference reference) {
		try (ModuleReader reader = reference.open()) {
			try (Stream<String> names = reader.list()) {
				// @formatter:off
                return names.filter(name -> name.endsWith(".class"))
                        .map(this::className)
                        .filter(classNameFilter)
                        .map(this::loadClass)
                        .filter(classTester)
                        .collect(Collectors.toList());
                // @formatter:on
			}
		}
		catch (IOException e) {
			throw new JUnitException("reading contents of " + reference + " failed", e);
		}
	}

	/** Convert resource name to binary class name. */
	private String className(String resourceName) {
		assert resourceName.endsWith(".class") : "resource name doesn't end with '.class': " + resourceName;
		resourceName = resourceName.substring(0, resourceName.length() - 6); // 6 = ".class".length()
		resourceName = resourceName.replace('/', '.');
		return resourceName;
	}

	/**
	 * Load class by its binary name.
	 *
	 * @see ClassLoader#loadClass(String)
	 */
	private Class<?> loadClass(String binaryName) {
		// TODO ClassLoaderUtils.getDefaultClassLoader(); ?
		ClassLoader classLoader = getClass().getClassLoader();
		try {
			return classLoader.loadClass(binaryName);
		}
		catch (ClassNotFoundException e) {
			throw new JUnitException("loading class with name '" + binaryName + "' failed", e);
		}
	}

}
