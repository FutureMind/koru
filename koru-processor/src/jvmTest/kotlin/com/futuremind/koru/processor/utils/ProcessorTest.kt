package com.futuremind.koru.processor.utils

import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.*
import org.junit.platform.commons.util.AnnotationUtils
import java.util.stream.Stream


@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(ProcessorTestExtension::class)
@TestTemplate
annotation class ProcessorTest(
    val kapt: Boolean = true,
    val ksp: Boolean = true
)

enum class ProcessorType {
    KAPT, KSP
}

class ProcessorTestExtension : TestTemplateInvocationContextProvider {

    override fun supportsTestTemplate(context: ExtensionContext?) = true

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
        val annotation = AnnotationUtils.findAnnotation(
            context.requiredTestMethod,
            ProcessorTest::class.java
        ).get()
        return listOfNotNull<TestTemplateInvocationContext>(
            if (annotation.kapt) ProcessorTestContext(ProcessorType.KAPT) else null,
            if (annotation.ksp) ProcessorTestContext(ProcessorType.KSP) else null,
        ).stream()
    }
}


class ProcessorTestContext(
    private val processorType: ProcessorType
) : TestTemplateInvocationContext {

    override fun getDisplayName(invocationIndex: Int) = processorType.name.lowercase()

    override fun getAdditionalExtensions(): MutableList<Extension> {
        return mutableListOf(
            ProcessorTypeParameterResolver(processorType)
        )
    }
}


class ProcessorTypeParameterResolver(private val type: ProcessorType) : ParameterResolver {

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean = parameterContext.parameter.type == ProcessorType::class.java

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ) = type
}