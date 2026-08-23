package com.jeromelens.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.jeromelens.app.R
import com.jeromelens.app.databinding.ActivityHistoryBinding
import com.jeromelens.app.util.Categories
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: ClipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ClipAdapter(
            onCopy = { text ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("JeromeLens", text))
                Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
            },
            onFavorite = { clip -> viewModel.toggleFavorite(clip) },
            onDelete = { clip -> viewModel.delete(clip) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.searchEditText.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim() ?: ""
            viewModel.search(query)
        }

        // Category chips
        setupCategoryChips()

        lifecycleScope.launch {
            viewModel.clips.collectLatest { list ->
                adapter.submitList(list)
                binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.categories.collectLatest { cats ->
                refreshCategoryChips(cats)
            }
        }
    }

    private fun setupCategoryChips() {
        val chipGroup = binding.categoryChipGroup ?: return
        chipGroup.removeAllViews()

        // "All" chip
        val allChip = Chip(this).apply {
            text = getString(R.string.all_categories)
            isCheckable = true
            isChecked = true
            setOnClickListener {
                viewModel.filterByCategory(null)
                uncheckOtherChips(this)
            }
        }
        chipGroup.addView(allChip)

        // Predefined categories for quick filter even before any data
        Categories.PREDEFINED.filter { it != Categories.UNCATEGORIZED }.forEach { cat ->
            val chip = Chip(this).apply {
                text = cat
                isCheckable = true
                setOnClickListener {
                    viewModel.filterByCategory(cat)
                    uncheckOtherChips(this)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun refreshCategoryChips(dynamicCats: List<String>) {
        val chipGroup = binding.categoryChipGroup ?: return
        // Keep All + predefined; add any extra custom categories from DB
        val existing = (0 until chipGroup.childCount)
            .mapNotNull { (chipGroup.getChildAt(it) as? Chip)?.text?.toString() }
            .toSet()

        dynamicCats.forEach { cat ->
            if (cat !in existing && cat != Categories.UNCATEGORIZED) {
                val chip = Chip(this).apply {
                    text = cat
                    isCheckable = true
                    setOnClickListener {
                        viewModel.filterByCategory(cat)
                        uncheckOtherChips(this)
                    }
                }
                chipGroup.addView(chip)
            }
        }
    }

    private fun uncheckOtherChips(selected: Chip) {
        val chipGroup = binding.categoryChipGroup ?: return
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip != selected) chip.isChecked = false
        }
        selected.isChecked = true
    }
}
