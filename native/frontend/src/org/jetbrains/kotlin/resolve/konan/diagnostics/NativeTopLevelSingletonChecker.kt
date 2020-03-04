/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics


import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.hasBackingField

object NativeTopLevelSingletonChecker : DeclarationChecker {
    private val threadLocalFqName = FqName("kotlin.native.concurrent.ThreadLocal")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        // @ThreadLocal on enum has no effect.
        if (descriptor is ClassDescriptor && DescriptorUtils.isEnumClass(descriptor)) {
            descriptor.annotations.findAnnotation(threadLocalFqName)?.let {
                val reportLocation = DescriptorToSourceUtils.getSourceFromAnnotation(it) ?: declaration
                context.trace.report(ErrorsNative.ENUM_THREAD_LOCAL_INAPPLICABLE.on(reportLocation))
            }
        }

        // Check variables inside singletons.
        if (descriptor !is PropertyDescriptor) return
        (descriptor.containingDeclaration as? ClassDescriptor)?.let { parent ->
            if (descriptor.isVar && DescriptorUtils.isEnumClass(parent)) {
                context.trace.report(ErrorsNative.VARIABLE_IN_ENUM.on(declaration))
            } else if (parent.kind.isSingleton) {
                parent.annotations.findAnnotation(threadLocalFqName) ?: run {
                    if (descriptor.isVar && declaration is KtProperty && declaration.delegate == null &&
                        descriptor.hasBackingField(context.trace.bindingContext) &&
                        descriptor.setter?.isDefault == true
                    ) {
                        context.trace.report(ErrorsNative.VARIABLE_IN_TOP_LEVEL_SINGLETON_WITHOUT_THREAD_LOCAL.on(declaration))
                    }
                }
            }
        }
    }
}
