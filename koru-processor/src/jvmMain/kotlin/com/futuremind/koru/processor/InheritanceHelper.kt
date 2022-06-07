package com.futuremind.koru.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement


@KotlinPoetMetadataPreview
internal fun Collection<Element>.sortByInheritance(
    classInspector: ClassInspector,
    processingEnv: ProcessingEnvironment
) = sortByInheritance(
    { it.getClassName(processingEnv) },
    { (it as TypeElement).toTypeSpec(classInspector).superinterfaces.keys }
)

@OptIn(KotlinPoetKspPreview::class)
fun Collection<KSClassDeclaration>.sortByInheritance() = sortByInheritance(
    { it.toClassName() },
    { it.getAllSuperTypes().map { it.toClassName() }.toList() }
)

private fun <E, N> Collection<E>.sortByInheritance(
    elementName: (element: E) -> N,
    superInterfaceNames: (element: E) -> Collection<N>
): List<E> {

    val vertices = this.map { element ->
        val name = elementName(element)
        Vertex(element, name)
    }

    val graph = Graph(vertices)

    vertices.forEach { vertex ->
        val superInterfacesNames = superInterfaceNames(vertex.element)
        superInterfacesNames.forEach { name ->
            val from = vertices.find { it.descriptor == name }
            from?.let { graph.addEdge(from, vertex) }
        }
    }

    return graph.topologicalOrder().map { it.element }
}


internal class Graph<E, D>(
    private val vertices: Collection<Vertex<E, D>>
) {

    private val adjacencyMap = vertices.map { it to mutableListOf<Vertex<E, D>>() }.toMap()

    fun addEdge(from: Vertex<E, D>, to: Vertex<E, D>) {
        adjacencyMap[from]!!.add(to)
    }

    fun topologicalOrder(): List<Vertex<E, D>> {

        var visitedNodes = 0
        val queue: Queue<Vertex<E, D>> = LinkedList()
        val orderedList = mutableListOf<Vertex<E, D>>()

        vertices.forEach { vertex ->
            adjacencyMap[vertex]!!.forEach { it.inDegree++ }
        }

        vertices.forEach { vertex ->
            if (vertex.inDegree == 0) queue.add(vertex)
        }

        while (!queue.isEmpty()) {
            val vertex = queue.remove()
            orderedList.add(vertex)
            visitedNodes++
            adjacencyMap[vertex]!!.forEach {
                it.inDegree--
                if (it.inDegree == 0) queue.add(it)
            }
        }

        if(visitedNodes != vertices.size) throw IllegalArgumentException("The graph contains cycles")

        return orderedList

    }

}

internal data class Vertex<E, D>(
    val element: E,
    val descriptor: D,
) {
    var inDegree = 0
}