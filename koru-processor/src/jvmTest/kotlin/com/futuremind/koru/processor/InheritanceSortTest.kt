package com.futuremind.koru.processor

import com.futuremind.koru.processor.InheritanceSortTest.Ver.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class InheritanceSortTest {

    @Test
    fun abc() {

        val a = vertex(A)
        val b = vertex(B)
        val c = vertex(C)
        val d = vertex(D)
        val e = vertex(E)
        val f = vertex(F)

        val graph = Graph(
            listOf(a, b, c, d, e, f)
        )

        graph.addEdge(a, c) //eg class C : A
        graph.addEdge(a, d)
        graph.addEdge(a, e)
        graph.addEdge(c, e)
        graph.addEdge(b, e)
        graph.addEdge(b, d)
        graph.addEdge(b, f)
        graph.addEdge(d, f)

        println("Order: ${graph.topologicalOrder().map { it.element }}")

    }

    private fun vertex(letter: Ver) = Vertex(letter, letter)

    enum class Ver {
        A,B,C,D,E,F
    }

}