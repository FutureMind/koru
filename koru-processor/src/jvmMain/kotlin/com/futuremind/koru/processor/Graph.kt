package com.futuremind.koru.processor

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
    val incomingEdgesDescriptors: Set<D>
) {
    var currentCount = incomingEdgesDescriptors.size
}


@KotlinPoetMetadataPreview
internal fun <E : Element> Collection<E>.sortByInheritance(classInspector: ClassInspector, processingEnv: ProcessingEnvironment): List<E> {

    val vertices = this.map { element ->
        val name = element.getClassName(processingEnv)
        val clazz = (element as TypeElement).toImmutableKmClass()
        Vertex(
            element,
            name,
            clazz.toTypeSpec(classInspector).superinterfaces.keys
        )
    }

    val graph = Graph(vertices)
    return graph.topologicalOrder().map { it.element }
}


class Graph<E, D>(
    private val vertices: Collection<Vertex<E, D>>
) {

    fun topologicalOrder(): List<Vertex<E, D>> {

        var visitedNodes = 0
        val queue : Queue<Vertex<E, D>> = LinkedList()
        val orderedList = mutableListOf<Vertex<E, D>>()

        vertices.forEach { vertex ->
            if(vertex.currentCount == 0) queue.add(vertex)
        }

        while(!queue.isEmpty()){
            val vertex = queue.remove()
            orderedList.add(vertex)
            visitedNodes++
            println("${vertex.descriptor} has incoming ${vertex.incomingEdgesDescriptors}")
            vertex.incomingEdgesDescriptors.forEach { descriptor ->
                vertices.find { it.descriptor == descriptor }!!.apply {
                    println("${this.descriptor} incoming to ${vertex.descriptor}")
                    currentCount--
                    if(currentCount == 0) queue.add(this)
                }
            }
        }

        return orderedList

    }
}