package com.github.korniloval.flameviewer.converters.calltraces

import com.github.korniloval.flameviewer.converters.tryCreate
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import java.io.File

object IntellijToCallTracesConverterFactory : ToCallTracesConverterFactory {
    private val EP_NAME = ExtensionPointName.create<ToCallTracesIdentifiedConverterFactory>("com.github.kornilovaL.flamegraphProfiler.toCallTracesConverterFactory")
    private val LOG = Logger.getInstance(IntellijToCallTracesConverterFactory::class.java)

    override fun create(file: File): ToCallTracesConverter? {
        return tryCreate(EP_NAME.extensions, file, LOG) as? ToCallTracesConverter
    }


    fun isSupported(file: File): String? {
        for (extension in EP_NAME.extensions) {
            if (extension.isSupported(file)) {
                return extension.id
            }
        }
        return null
    }
}