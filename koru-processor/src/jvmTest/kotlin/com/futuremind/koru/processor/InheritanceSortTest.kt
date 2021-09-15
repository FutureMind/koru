package com.futuremind.koru.processor

import com.futuremind.koru.processor.InheritanceSortTest.Ver.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class InheritanceSortTest {

    @Test
    fun abc() {

        val graph = Graph(
            listOf(
                vertex(A),
                vertex(B),
                vertex(C, setOf(A)),
                vertex(D, setOf(A, B)),
                vertex(E, setOf(A, B, C)),
                vertex(F, setOf(D, B)),
            )
        )

        println("Order: ${graph.topologicalOrder().map { it.element }}")

    }

    private fun vertex(letter: Ver, incomingEdges: Set<Ver> = setOf()) = Vertex(letter, letter, incomingEdges)

    enum class Ver {
        A,B,C,D,E,F
    }

}