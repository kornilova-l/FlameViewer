package com.github.korniloval.flameviewer.server.handlers

import com.github.kornilova_l.flamegraph.proto.TreeProtos.Tree
import com.github.korniloval.flameviewer.converters.trees.maximumNodesCount
import com.github.korniloval.flameviewer.FlameLogger

import com.github.korniloval.flameviewer.server.RequestHandlerBase
import com.github.korniloval.flameviewer.server.ServerUtil.sendProto
import com.sun.xml.internal.ws.handler.HandlerException
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import java.io.File
import java.util.*


abstract class TreeHandler(protected val logger: FlameLogger, private val findFile: FindFile) : RequestHandlerBase() {

    abstract fun getTree(file: File, decoder: QueryStringDecoder): Tree?

    final override fun processGet(request: HttpRequest, ctx: ChannelHandlerContext): Boolean {
        val decoder = QueryStringDecoder(request.uri())
        val file = findFile(getFileName(decoder)) ?: throw HandlerException("File not found. Uri: ${decoder.uri()}")
        doProcess(ctx, file, decoder)
        return true
    }

    protected open fun doProcess(ctx: ChannelHandlerContext, file: File, decoder: QueryStringDecoder) {
        val tree = getTree(file, decoder)
        if (tree != null && tree.treeInfo.nodesCount > maximumNodesCount) {
            val cutTree = cutTree(tree)
            sendProto(ctx, cutTree, logger)
        } else {
            sendProto(ctx, tree, logger)
        }
    }

    private fun cutTree(tree: Tree): Tree {
        val lastLayer = getLastAcceptedLayerIndex(tree.baseNode)
        val treeBuilder = Tree.newBuilder()
        cutTree(tree.baseNode, treeBuilder.baseNodeBuilder, 1, lastLayer)
        @Suppress("UsePropertyAccessSyntax")
        treeBuilder.setTreeInfo(tree.treeInfo)
                .setDepth(tree.depth)
                .setWidth(tree.width)
                .setVisibleDepth(lastLayer)
        return treeBuilder.build()
    }

    companion object {
        /**
         * Returns index of last accepted layer.
         * If returned index is 10 it means that
         * first 10 layers of tree (not including base node layer)
         * contain less than [maximumNodesCount] nodes.
         * If you add 11th layer then there will be more than
         * [maximumNodesCount] nodes.
         */
        internal fun getLastAcceptedLayerIndex(firstNode: Tree.Node): Int {
            var nodesCount = -1 // do not count base node
            var currentLayerIndex = -1 // do not count base node layer
            /* we need to know when layer ends to update currentLayerIndex */
            var currentLayer = LinkedList<Tree.Node>()
            var nextLayer = LinkedList<Tree.Node>()
            currentLayer.add(firstNode)
            while (!currentLayer.isEmpty()) {
                val node = currentLayer.removeFirst()
                nodesCount++
                if (nodesCount > maximumNodesCount) {
                    return currentLayerIndex
                }
                for (child in node.nodesList) {
                    nextLayer.add(child)
                }
                if (currentLayer.isEmpty()) {
                    currentLayer = nextLayer
                    nextLayer = LinkedList()
                    currentLayerIndex++
                }
            }
            return currentLayerIndex
        }

        internal fun cutTree(node: Tree.Node, nodeBuilder: Tree.Node.Builder, currentLayer: Int, lastAcceptedLayer: Int) {
            if (currentLayer > lastAcceptedLayer) {
                return
            }
            for (child in node.nodesList) {
                val newChild = Tree.Node.newBuilder()
                        .setWidth(child.width)
                        .setOffset(child.offset)
                        .setNodeInfo(child.nodeInfo)
                nodeBuilder.addNodes(newChild)
                val addedChild = nodeBuilder.getNodesBuilder(nodeBuilder.nodesBuilderList.size - 1)
                cutTree(child, addedChild, currentLayer + 1, lastAcceptedLayer)
            }
        }

    }
}