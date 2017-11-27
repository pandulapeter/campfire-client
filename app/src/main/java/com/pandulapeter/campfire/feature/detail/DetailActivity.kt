package com.pandulapeter.campfire.feature.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import com.pandulapeter.campfire.DetailBinding
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.data.repository.SongInfoRepository
import com.pandulapeter.campfire.feature.shared.CampfireActivity
import javax.inject.Inject

/**
 * Displays the chords and lyrics of a single song per screen. The user can cycle through all the
 * songs in the category if they started this screen from Downloads of Favorites.
 *
 * //TODO: Add option to add / remove song from Favorites.
 *
 * Controlled by [DetailViewModel].
 */
class DetailActivity : CampfireActivity<DetailBinding, DetailViewModel>(R.layout.activity_detail) {
    @Inject lateinit var songInfoRepository: SongInfoRepository
    override val viewModel by lazy { DetailViewModel(supportFragmentManager, songInfoRepository, intent.currentId, intent.ids) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setup the toolbar.
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Setup the view pager.
        //TODO: Replace with a better solution, pay attention to state saving.
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) = Unit

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit

            override fun onPageSelected(position: Int) {
                binding.viewModel?.updateToolbar(intent.ids[position])
            }
        })
        binding.viewPager.currentItem = intent.ids.indexOf(intent.currentId)
    }

    companion object {
        private const val CURRENT_ID = "current_id"
        private const val IDS = "ids"
        private val Intent.currentId
            get() = getStringExtra(CURRENT_ID)
        private val Intent.ids
            get() = getStringArrayExtra(IDS).toList()

        fun getStartIntent(context: Context, currentId: String, ids: List<String> = listOf()): Intent = Intent(context, DetailActivity::class.java)
            .putExtra(CURRENT_ID, currentId)
            .putExtra(IDS, ids.toTypedArray())
    }
}