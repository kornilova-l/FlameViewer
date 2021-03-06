package com.github.kornilova203.flameviewer.server

import com.github.kornilova203.flameviewer.LoggerAdapter
import com.github.kornilova203.flameviewer.PluginFileManager
import com.github.kornilova203.flameviewer.converters.calltraces.ToCallTracesConverterFactoryIntellij
import com.github.kornilova203.flameviewer.converters.calltree.ToCallTreeConverterFactoryIntellij
import com.github.kornilova203.flameviewer.converters.trees.ToTreesSetConverterFactory
import com.github.kornilova203.flameviewer.converters.trees.TreeType.BACK_TRACES
import com.github.kornilova203.flameviewer.converters.trees.TreeType.CALL_TRACES
import com.github.kornilova203.flameviewer.server.ServerUtil.validateMaxNumOfVisibleNodes
import com.github.kornilova203.flameviewer.server.handlers.*
import com.intellij.openapi.diagnostic.Logger
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.annotations.TestOnly
import org.jetbrains.ide.HttpRequestHandler

class IntellijRequestHandler : HttpRequestHandler() {
    private val logger = LoggerAdapter(Logger.getInstance(IntellijRequestHandler::class.java))
    private val treeManager = IntellijTreeManager(ToTreesSetConverterFactory(ToCallTreeConverterFactoryIntellij, ToCallTracesConverterFactoryIntellij))
    private val findFile: FindFile = { name -> PluginFileManager.getLogFile(name) }
    private var optionsProvider: ServerOptionsProviderImpl = ServerOptionsProviderImpl(ServerOptions(DEFAULT_MAX_NUM_OF_VISIBLE_NODES))

    private val handler = DelegatingRequestHandler(
            mapOf(FILE_LIST to IntellijFileListHandler(logger),
                    HOT_SPOTS_JSON to HotSpotsHandler(treeManager, logger, findFile),
                    SERIALIZED_CALL_TREE to CallTreeHandler(treeManager, logger, findFile),
                    CALL_TREE_PREVIEW to TreesPreviewHandler(treeManager, logger, findFile),
                    SERIALIZED_CALL_TRACES to CallTracesHandler(treeManager, logger, optionsProvider, findFile),
                    SERIALIZED_BACK_TRACES to BackTracesHandler(treeManager, logger, optionsProvider, findFile),
                    CALL_TREE_COUNT to CallTreeCountMethods(treeManager, logger, findFile),
                    CALL_TRACES_COUNT to AccumulativeTreesCountMethods(treeManager, CALL_TRACES, logger, findFile),
                    BACK_TRACES_COUNT to AccumulativeTreesCountMethods(treeManager, BACK_TRACES, logger, findFile),
                    DOES_FILE_EXIST to DoesFileExistHandler(findFile, logger),

                    CALL_TREE_PAGE to HtmlHandler,
                    CALL_TRACES_PAGE to HtmlHandler,
                    BACK_TRACES_PAGE to HtmlHandler,
                    HOT_SPOTS_PAGE to HtmlHandler,

                    CONNECTION_ALIVE to ConnectionAliveHandler(treeManager),
                    FILE to FileHandler(PluginFileManager, FileUploader(), logger),
                    UNDO_DELETE_FILE to UndoDeleteFileHandler(PluginFileManager, logger),

                    SUPPORTS_CLEARING_CACHES to BooleanResultHandler(true, logger),
                    SUPPORTS_FILE_LIST to BooleanResultHandler(true, logger)),

            listOf(CSS_PATTERN to StaticHandler("text/css"),
                    JS_PATTERN to StaticHandler("text/javascript"),
                    FONT_PATTERN to StaticHandler("application/octet-stream"),
                    PNG_PATTERN to StaticHandler("image/png")))

    override fun process(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): Boolean {
        return handler.process(request, context)
    }

    override fun isSupported(request: FullHttpRequest): Boolean = when (request.method()) {
        HttpMethod.POST -> true
        HttpMethod.GET -> true
        HttpMethod.DELETE -> true
        else -> false
    }

    @TestOnly
    fun setMaxNumOfVisibleNodes(value: Int) {
        val v = validateMaxNumOfVisibleNodes(value, logger) ?: return
        optionsProvider.options = optionsProvider.options.copy(maxNumOfVisibleNodes = v)
    }
}
