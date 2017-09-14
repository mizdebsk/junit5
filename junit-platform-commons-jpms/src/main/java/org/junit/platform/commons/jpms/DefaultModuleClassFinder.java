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

import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ModuleClassFinder;
import org.junit.platform.commons.util.Preconditions;

/**
 * Default module class finder implementation.
 *
 * @see ModuleClassFinder
 * @since 1.1
 */
@API(status = API.Status.INTERNAL)
public class DefaultModuleClassFinder implements ModuleClassFinder {

	@Override
	public List<Class<?>> findAllClassesInModule(String moduleName, Predicate<Class<?>> classTester,
			Predicate<String> classNameFilter) {
		Preconditions.notBlank(moduleName, "module name must not be null or blank");
		Preconditions.notNull(classTester, "class tester predicate must not be null");
		Preconditions.notNull(classTester, "class name filter predicate must not be null");

		// collect module references
		Stream<ResolvedModule> stream = ModuleLayer.boot().configuration().modules().stream();
		if (!ModuleClassFinder.ALL_MODULE_PATH.equals(moduleName)) {
			stream = stream.filter(module -> module.name().equals(moduleName));
		}
		List<ModuleReference> references = stream.map(ResolvedModule::reference).collect(Collectors.toList());

		// scan for classes
		ModuleReferenceScanner scanner = new ModuleReferenceScanner(classTester, classNameFilter);
		List<Class<?>> classes = new ArrayList<>();
		for (ModuleReference reference : references) {
			classes.addAll(scanner.scan(reference));
		}
		return Collections.unmodifiableList(classes);
	}

}
