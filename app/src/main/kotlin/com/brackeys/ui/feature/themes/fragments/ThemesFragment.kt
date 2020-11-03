/*
 * Copyright 2020 Brackeys IDE contributors.
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

package com.brackeys.ui.feature.themes.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
import com.brackeys.ui.R
import com.brackeys.ui.databinding.FragmentThemesBinding
import com.brackeys.ui.domain.model.theme.ThemeModel
import com.brackeys.ui.feature.base.fragments.BaseFragment
import com.brackeys.ui.feature.themes.adapters.ThemeAdapter
import com.brackeys.ui.feature.themes.utils.GridSpacingItemDecoration
import com.brackeys.ui.feature.themes.viewmodel.ThemesViewModel
import com.brackeys.ui.utils.extensions.getAssetFileText
import com.brackeys.ui.utils.extensions.hasExternalStorageAccess
import com.jakewharton.rxbinding3.appcompat.queryTextChangeEvents
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ThemesFragment : BaseFragment(R.layout.fragment_themes), ThemeAdapter.ThemeInteractor {

    private val viewModel: ThemesViewModel by viewModels()

    private lateinit var navController: NavController
    private lateinit var binding: FragmentThemesBinding
    private lateinit var adapter: ThemeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentThemesBinding.bind(view)
        observeViewModel()

        navController = findNavController()
        val gridLayoutManager = binding.recyclerView.layoutManager as GridLayoutManager
        val gridSpacingDecoration = GridSpacingItemDecoration(8, gridLayoutManager.spanCount)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(gridSpacingDecoration)
        binding.recyclerView.adapter = ThemeAdapter(this)
            .also { adapter = it }

        binding.actionAdd.setOnClickListener {
            val destination = ThemesFragmentDirections.toNewThemeFragment(null)
            navController.navigate(destination)
        }

        viewModel.fetchThemes()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_themes, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        if (viewModel.searchQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(viewModel.searchQuery, false)
        }

        val spinnerItem = menu.findItem(R.id.spinner)
        val spinnerView = spinnerItem?.actionView as? AppCompatSpinner

        spinnerView?.adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.language_names,
            android.R.layout.simple_spinner_dropdown_item
        )
        spinnerView?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val path = requireContext().getStringArray(R.array.language_paths)[position]
                val extension = requireContext().getStringArray(R.array.language_extensions)[position]
                adapter.codeSnippet = requireContext().getAssetFileText(path) to extension
            }
        }

        searchView
            .queryTextChangeEvents()
            .skipInitialValue()
            .debounce(200, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                viewModel.searchQuery = it.queryText.toString()
                viewModel.fetchThemes()
            }
            .disposeOnFragmentDestroyView()
    }

    override fun selectTheme(themeModel: ThemeModel) {
        viewModel.selectTheme(themeModel)
    }

    override fun exportTheme(themeModel: ThemeModel) {
        if (requireContext().hasExternalStorageAccess()) {
            viewModel.exportTheme(themeModel)
        } else {
            showToast(R.string.message_access_required)
        }
    }

    override fun editTheme(themeModel: ThemeModel) {
        if (themeModel.isExternal) {
            val destination = ThemesFragmentDirections.toNewThemeFragment(themeModel.uuid)
            navController.navigate(destination)
        }
    }

    override fun removeTheme(themeModel: ThemeModel) {
        if (themeModel.isExternal) {
            viewModel.removeTheme(themeModel)
        }
    }

    override fun showInfo(themeModel: ThemeModel) {
        showToast(text = themeModel.description)
    }

    private fun observeViewModel() {
        viewModel.toastEvent.observe(viewLifecycleOwner) {
            showToast(it)
        }
        viewModel.themesEvent.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.emptyView.isVisible = it.isEmpty()
        }
        viewModel.selectEvent.observe(viewLifecycleOwner) {
            showToast(text = getString(R.string.message_selected, it))
        }
        viewModel.exportEvent.observe(viewLifecycleOwner) {
            showToast(
                text = getString(R.string.message_theme_exported, it),
                duration = Toast.LENGTH_LONG
            )
        }
        viewModel.removeEvent.observe(viewLifecycleOwner) {
            showToast(text = getString(R.string.message_theme_removed, it))
        }
    }
}