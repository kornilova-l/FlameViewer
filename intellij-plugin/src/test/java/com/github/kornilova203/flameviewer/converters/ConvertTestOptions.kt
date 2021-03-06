package com.github.kornilova203.flameviewer.converters

data class ConvertTestOptions(val path: List<Int>,
                              val className: String?,
                              val methodName: String?,
                              val description: String?,
                              val fileName: String?,
                              val include: String?,
                              val includeStacktrace: Boolean?,
                              val maxNumOfVisibleNodes: Int?)

fun opt(path: List<Int> = ArrayList(),
        className: String? = null,
        methodName: String? = null,
        description: String? = null,
        fileName: String? = null,
        include: String? = null,
        includeStacktrace: Boolean? = null,
        maxNumOfVisibleNodes: Int? = null) = ConvertTestOptions(path, className, methodName, description, fileName, include, includeStacktrace, maxNumOfVisibleNodes)
