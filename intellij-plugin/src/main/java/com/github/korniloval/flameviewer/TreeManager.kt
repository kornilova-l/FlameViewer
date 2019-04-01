package com.github.korniloval.flameviewer

import com.github.kornilova_l.flamegraph.proto.TreeProtos
import com.github.kornilova_l.flamegraph.proto.TreesPreviewProtos.TreesPreview
import com.github.kornilova_l.flamegraph.proto.TreesProtos
import com.github.korniloval.flameviewer.converters.calltraces.IntellijToCallTracesConverterFactory
import com.github.korniloval.flameviewer.converters.calltree.IntellijToCallTreeConverterFactory
import com.github.korniloval.flameviewer.converters.trees.Filter
import com.github.korniloval.flameviewer.converters.trees.TreeType
import com.github.korniloval.flameviewer.converters.trees.TreesSet
import com.github.korniloval.flameviewer.converters.trees.TreesSetImpl
import com.github.korniloval.flameviewer.converters.trees.hotspots.HotSpot
import com.intellij.openapi.diagnostic.Logger
import java.io.File

object TreeManager {
    private var currentFile: File? = null
    @Volatile
    private var currentTreesSet: TreesSet? = null
    private var lastUpdate: Long = 0

    init {
        val watchLastUpdate = Thread {
            while (true) {
                try {
                    Thread.sleep(10000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                if (longTimePassedSinceUpdate()) {
                    removeTreesSet()
                }
            }
        }
        watchLastUpdate.isDaemon = true
        watchLastUpdate.start()
    }

    private fun removeTreesSet() {
        currentTreesSet = null
        currentFile = null
        lastUpdate = System.currentTimeMillis()
    }

    @Synchronized
    private fun longTimePassedSinceUpdate(): Boolean {
        return System.currentTimeMillis() - lastUpdate >= 30000
    }

    @Synchronized
    fun getCallTree(logFile: File?,
                    filter: Filter?,
                    threadsIds: List<Int>?): TreesProtos.Trees? {
        logFile ?: return null
        updateTreesSet(logFile)
        val currentTreesSet = currentTreesSet ?: return null
        return currentTreesSet.getCallTree(filter, threadsIds)
    }

    private fun updateTreesSet(logFile: File) {
        if (currentFile == null || logFile.absolutePath != currentFile!!.absolutePath) {
            currentFile = logFile
            /* try to convert to call tree */
            val callTree = IntellijToCallTreeConverterFactory.create(logFile)?.convert()
            currentTreesSet =
                    if (callTree != null) {
                        TreesSetImpl(callTree)
                    } else {
                        /* try to convert to call traces */
                        val callTraces = IntellijToCallTracesConverterFactory.create(logFile)?.convert()
                        if (callTraces == null) {
                            LOG.error("Cannot convert file: ${logFile.absolutePath}")
                            return
                        }
                        TreesSetImpl(callTraces)
                    }
        }
    }

    @Synchronized
    fun getTree(logFile: File?, treeType: TreeType, filter: Filter?): TreeProtos.Tree? {
        logFile ?: return null
        updateTreesSet(logFile)
        val currentTreesSet = currentTreesSet ?: return null
        return currentTreesSet.getTree(treeType, filter)
    }

    @Synchronized
    fun getTree(logFile: File?,
                treeType: TreeType,
                className: String,
                methodName: String,
                desc: String,
                filter: Filter?): TreeProtos.Tree? {
        logFile ?: return null
        updateTreesSet(logFile)
        val currentTreesSet = currentTreesSet ?: return null
        return currentTreesSet.getTree(treeType, className, methodName, desc, filter)

    }

    @Synchronized
    fun getHotSpots(logFile: File?): List<HotSpot>? {
        logFile ?: return null
        updateTreesSet(logFile)
        val currentTreesSet = currentTreesSet ?: return null
        return currentTreesSet.getHotSpots()
    }

    @Synchronized
    fun updateLastTime() {
        lastUpdate = System.currentTimeMillis()
    }

    @Synchronized
    fun getCallTreesPreview(logFile: File?, filter: Filter?): TreesPreview? {
        logFile ?: return null
        updateTreesSet(logFile)
        val currentTreesSet = currentTreesSet ?: return null
        return currentTreesSet.getTreesPreview(filter)
    }

    private val LOG = Logger.getInstance(PluginFileManager::class.java)
}

