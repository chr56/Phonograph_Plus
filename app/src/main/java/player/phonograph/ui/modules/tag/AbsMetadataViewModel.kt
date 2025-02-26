/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import player.phonograph.model.metadata.EditAction
import androidx.lifecycle.ViewModel
import android.content.Context
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class AbsMetadataViewModel : ViewModel() {

    private val _editable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val editable get() = _editable.asStateFlow()

    fun enterEditMode() {
        if (saveJob?.isActive == true) return
        _editable.update { true }
    }

    fun exitEditMode() {
        _editable.update { false }
    }

    protected var _pendingEditRequests: MutableList<EditAction> = mutableListOf()
    protected val pendingEditRequests: List<EditAction> get() = EditAction.merge(_pendingEditRequests)
    val hasChanges: Boolean get() = _pendingEditRequests.isNotEmpty()

    protected fun enqueueEditRequest(action: EditAction) {
        _pendingEditRequests.add(action)
    }

    abstract fun submitEditEvent(context: Context, event: TagEditEvent)

    protected var saveJob: Job? = null
    abstract fun save(context: Context)

    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)
    val coverImageDetailDialogState = MaterialDialogState(false)

    abstract fun generateMetadataDifference(): MetadataChanges
}