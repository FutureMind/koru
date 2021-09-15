package com.futuremind.koru.processor

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

data class Vertex<E, D>(
    val element: E,
    val descriptor: D,
) {
    var inDegree = 0
}


@KotlinPoetMetadataPreview
internal fun Collection<Element>.sortByInheritance(
    classInspector: ClassInspector,
    processingEnv: ProcessingEnvironment
): List<Element> {

    val vertices = this.map { element ->
        val name = element.getClassName(processingEnv) as TypeName
        val clazz = (element as TypeElement).toImmutableKmClass()
        Vertex<Element, TypeName>(
            element,
            name,
//            clazz.toTypeSpec(classInspector).superinterfaces.keys
        )
    }

    val graph = Graph(vertices)

    vertices.forEach { vertex ->
        val superInterfacesNames = (vertex.element as TypeElement).toImmutableKmClass()
            .toTypeSpec(classInspector).superinterfaces.keys
        superInterfacesNames.forEach { name ->
            val from = vertices.find { it.descriptor == name }!!
            graph.addEdge(from, vertex)
        }
    }

    return graph.topologicalOrder().map { it.element }
}


class Graph<E, D>(
    private val vertices: Collection<Vertex<E, D>>
) {

    private val adjacencyMap = vertices.map { it to mutableListOf<Vertex<E, D>>() }.toMap()

    fun addEdge(from: Vertex<E, D>, to: Vertex<E, D>) {
        adjacencyMap[from]!!.add(to)
    }

//    fun addEdge(from: D, to: D) {
//        adjacencyMap[from]!!.add(to)
//    }

    fun topologicalOrder(): List<Vertex<E, D>> {

        var visitedNodes = 0
        val queue: Queue<Vertex<E, D>> = LinkedList()
        val orderedList = mutableListOf<Vertex<E, D>>()

//        vertices.forEach { vertex ->
//            vertex.outgoingEdgesDescriptors.forEach {
//                if (vertex.descriptor == it) vertex.inDegree++
//            }
//        }

        vertices.forEach { vertex ->
            if (vertex.inDegree == 0) queue.add(vertex)
        }

        while (!queue.isEmpty()) {
            val vertex = queue.remove()
            orderedList.add(vertex)
            visitedNodes++
            println("${vertex.descriptor} has incoming ${adjacencyMap[vertex]}")
//            vertex.outgoingEdgesDescriptors.forEach { descriptor ->
//                vertices.find { it.descriptor == descriptor }!!.apply {
//                    println("${this.descriptor} incoming to ${vertex.descriptor}")
//                    inDegree--
//                    if (inDegree == 0) queue.add(this)
//                }
//            }
        }

        return orderedList

    }

}