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
import java.lang.module.ResolvedModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ModuleUtils;
import org.junit.platform.commons.util.Preconditions;

/**
 * Default module class finder implementation.
 *
 * @see ModuleUtils.ClassFinder
 */
@API(status = API.Status.INTERNAL)
public class ModuleClassFinder implements ModuleUtils.ClassFinder {

	@Override
	public List<Class<?>> findAllClassesInModule(String moduleName, Predicate<Class<?>> classTester,
			Predicate<String> classNameFilter) {
		// @formatter:off
		List<ModuleReference> references = ModuleLayer.boot()
				.configuration()
				.modules()
				.stream()
				.filter(module -> module.name().equals(moduleName))
				.map(ResolvedModule::reference)
				.collect(Collectors.toList());
		// @formatter:on

		List<Class<?>> classes = new ArrayList<>();
		for (ModuleReference reference : references) {
			scan(classes, reference, classTester, classNameFilter);
		}
		return Collections.unmodifiableList(classes);
	}

	/**
	 * Scan module reference for classes that potentially contain testable methods.
	 */
	private void scan(List<Class<?>> classes, ModuleReference reference, Predicate<Class<?>> classTester,
			Predicate<String> classNameFilter) {

		try (ModuleReader reader = reference.open()) {
			try (Stream<String> names = reader.list()) {
				// @formatter:off
				names.filter(name -> name.endsWith(".class"))
						.map(this::className)
						.filter(classNameFilter)
						.map(this::loadClass)
						.filter(classTester)
						.forEach(classes::add);
				// @formatter:on
			}
		}
		catch (IOException e) {
			throw new JUnitException("reading contents of " + reference + " failed", e);
		}
	}

	/** Convert resource name to binary class name. */
	private String className(String resourceName) {
		Preconditions.notBlank(resourceName, "resource name must not be null or blank");
		Preconditions.condition(resourceName.endsWith(".class"), "resource doesn't end with '.class'");

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
