package com.blacksquircle.ui.feature.explorer.ui.viewstate

import com.blacksquircle.ui.core.ui.viewstate.ViewState
import com.blacksquircle.ui.feature.explorer.data.utils.BufferType
import com.blacksquircle.ui.filesystem.base.model.FileModel
import java.util.*

sealed class ExplorerViewState : ViewState() {

    object Stub : ExplorerViewState()

    data class Data(
        val breadcrumbs: List<FileModel>,
        val selection: List<FileModel>,
        val bufferType: BufferType,
        val patch: String = UUID.randomUUID().toString(),
    ) : ExplorerViewState()
}