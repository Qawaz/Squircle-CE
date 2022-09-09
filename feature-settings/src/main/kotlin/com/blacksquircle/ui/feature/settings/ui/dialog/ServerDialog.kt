/*
 * Copyright 2022 Squircle CE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blacksquircle.ui.feature.settings.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.blacksquircle.ui.feature.settings.R
import com.blacksquircle.ui.feature.settings.databinding.DialogServerBinding
import com.blacksquircle.ui.feature.settings.ui.viewmodel.SettingsViewModel
import com.blacksquircle.ui.filesystem.base.model.ServerModel
import java.util.*

class ServerDialog : DialogFragment() {

    private val viewModel by hiltNavGraphViewModels<SettingsViewModel>(R.id.settings_graph)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog(requireContext()).show {
            title(R.string.pref_add_server_title)
            customView(R.layout.dialog_server)

            val binding = DialogServerBinding.bind(getCustomView())
            negativeButton(R.string.action_cancel)
            positiveButton(R.string.action_save) {
                val serverModel = ServerModel(
                    uuid = UUID.randomUUID().toString(),
                    scheme = "ftp://", // TODO
                    name = binding.inputServerName.text.toString(),
                    address = binding.inputServerAddress.text.toString(),
                    port = binding.inputServerPort.text.toString().toLong(),
                    username = binding.inputUsername.text.toString(),
                    password = binding.inputPassword.text.toString(),
                )
                viewModel.saveServer(serverModel)
            }
        }
    }
}