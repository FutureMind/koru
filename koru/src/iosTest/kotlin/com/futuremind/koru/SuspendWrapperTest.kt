package com.futuremind.koru

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SuspendWrapperTest {

    class SomeMutableClass {

        var someMutableString: String? = null

        suspend fun doSthSuspending(): String {
            someMutableString = "mutatedInSuspend"
            delay(100)
            return someMutableString!!
        }

        fun runSomeFlow() = flow {
            someMutableString = "mutatedInFlow"
            delay(100)
            emit(someMutableString)
        }

    }

    class Wrapper(private val wrapped: SomeMutableClass) {
        fun doSthSuspending() = SuspendWrapper(null) { wrapped.doSthSuspending() }
        fun runSomeFlow() = FlowWrapper(null, wrapped.runSomeFlow())
    }

    @Test
    fun testWrappedClassNotFrozenBySuspendWrapper() = runBlocking {

        val wrapper = Wrapper(SomeMutableClass())

        val suspendWrapper = wrapper.doSthSuspending()

        val job = suspendWrapper.subscribe(
            this,
            onSuccess = { assertEquals("mutatedInSuspend", it) },
            onThrow = { throw it }
        )

        job.join()

    }

    @Test
    fun testWrappedClassNotFrozenByFlowWrapper() = runBlocking {

        val wrapper = Wrapper(SomeMutableClass())

        val flowWrapper = wrapper.runSomeFlow()

        val job = flowWrapper.subscribe(
            this,
            onEach = { assertEquals("mutatedInFlow", it) },
            onComplete = {},
            onThrow = { throw it }
        )

        job.join()

    }

}