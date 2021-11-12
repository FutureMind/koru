package com.futuremind.koru.processor

import com.futuremind.koru.processor.InheritanceSortTest.Ver.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException

internal class InheritanceSortTest {

    private val a = vertex(A)
    private val b = vertex(B)
    private val c = vertex(C)
    private val d = vertex(D)
    private val e = vertex(E)
    private val f = vertex(F)

    @Test
    fun `should just return zero item graph`() {
        val graph = Graph(listOf<Vertex<Ver, Ver>>())
        test(graph, expectedSorted = listOf())
    }

    @Test
    fun `should just return one item graph`() {
        val graph = Graph(listOf(a))
        test(graph, expectedSorted = listOf(a))
    }

    @Test
    fun `should topologically sort a simple graph`() {
        val graph = Graph(listOf(a, b))
        graph.addEdge(a, b)
        test(graph, expectedSorted = listOf(a, b))
    }

    @Test
    fun `should topologically sort a simple graph (reversed)`() {
        val graph = Graph(listOf(a, b))
        graph.addEdge(b, a)
        test(graph, expectedSorted = listOf(b, a))
    }

    @Test
    fun `should topologically sort multilevel graph`() {

        val graph = Graph(listOf(a, b, c, d, e, f))

        graph.addEdge(a, c) //eg class C : A
        graph.addEdge(a, d)
        graph.addEdge(a, e)
        graph.addEdge(c, e)
        graph.addEdge(b, e)
        graph.addEdge(b, d)
        graph.addEdge(b, f)
        graph.addEdge(d, f)

        test(
            graph = graph,
            expectedSorted = listOf(a, b, c, d, e, f)
        )

    }

    @Test
    fun `should throw on cyclic graph`() {
        val graph = Graph(listOf(a, b))
        graph.addEdge(a, b)
        graph.addEdge(b, a)
        assertThrows(IllegalArgumentException::class.java) { graph.topologicalOrder() }
    }

    @Test
    fun `should throw on cyclic graph (larger)`() {
        val graph = Graph(listOf(a, b, c, d))
        graph.addEdge(a, b)
        graph.addEdge(b, c)
        graph.addEdge(c, d)
        graph.addEdge(d, a)
        assertThrows(IllegalArgumentException::class.java) { graph.topologicalOrder() }
    }

    private fun test(
        graph: Graph<Ver, Ver>,
        expectedSorted: List<Vertex<Ver, Ver>>
    ) = assertEquals(
        expectedSorted,
        graph.topologicalOrder()
    )

    private fun vertex(letter: Ver) = Vertex(letter, letter)

    enum class Ver {
        A, B, C, D, E, F
    }

}