package mg.watched.animes.animedetail

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import mg.watched.animes.R
import mg.watched.animes.databinding.AnimeDetailFragmentBinding
import mg.watched.animes.editanimeliststatus.EditAnimeListStatusFragment
import mg.watched.animes.utils.AnimeAnimations
import mg.watched.animes.utils.formatKindSeasonAiring
import mg.watched.animes.utils.formatRating
import mg.watched.core.base.BaseFragment
import mg.watched.core.utils.exhaustive
import mg.watched.core.utils.withArguments
import mg.watched.core.viewmodel.observeEvents
import mg.watched.data.anime.network.models.AlternativeTitles
import mg.watched.data.anime.network.models.Anime
import mg.watched.data.anime.network.models.WatchStatus
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class AnimeDetailFragment : BaseFragment(R.layout.anime_detail_fragment) {

    companion object {
        private const val ARG_ANIME: String = "arg_anime"

        fun newInstance(anime: Anime): AnimeDetailFragment = AnimeDetailFragment().withArguments(
            ARG_ANIME to anime
        )
    }

    // region Properties

    private val viewModel: AnimeDetailViewModel by viewModel { parametersOf(anime) }

    private val anime: Anime
        get() = requireArguments().getParcelable(ARG_ANIME)!!

    // endregion

    // region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            containerColor = MaterialColors.getColor(requireActivity(), android.R.attr.windowBackground, "")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding: AnimeDetailFragmentBinding = AnimeDetailFragmentBinding.bind(requireView())
        initUI(binding, anime)

        viewModel.viewStates().observe(viewLifecycleOwner) { bindViewState(it, binding) }
        viewModel.actionEvents().observeEvents(viewLifecycleOwner) { handleActionEvent(it) }
    }

    private fun initUI(binding: AnimeDetailFragmentBinding, anime: Anime) {
        requireView().transitionName = AnimeAnimations.getAnimeMasterDetailTransitionName(anime)

        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
    }

    // endregion

    // region ViewStates, NavigationEvents and ActionEvents

    private fun bindViewState(viewState: AnimeDetailViewState, binding: AnimeDetailFragmentBinding) {
        val anime: Anime = viewState.anime

        binding.tvTitle.text = anime.title
        binding.tvKindSeasonAiring.text = anime.formatKindSeasonAiring(requireContext())

        Glide.with(binding.ivIllustration)
            .load(anime.mainPicture.mediumUrl)
            .into(binding.ivIllustration)

        if (anime.myListStatus == null) {
            binding.vgAddToWatchlist.root.isVisible = true
            binding.vgWatchStatus.root.isVisible = false

            bindAddToWatchlist(binding, anime)
        } else {
            binding.vgAddToWatchlist.root.isVisible = false
            binding.vgWatchStatus.root.isVisible = true

            bindWatchStatus(binding, anime)
        }

        val alternativeTitles: AlternativeTitles = anime.alternativeTitles
        val enAndJaTitlesText: String = when {
            alternativeTitles.englishTitle.isNotBlank() && alternativeTitles.japaneseTitle.isNotBlank() ->
                "${alternativeTitles.englishTitle},\n${alternativeTitles.japaneseTitle}"
            alternativeTitles.englishTitle.isNotBlank() -> alternativeTitles.englishTitle
            alternativeTitles.japaneseTitle.isNotBlank() -> alternativeTitles.japaneseTitle
            else -> ""
        }
        val alternativeTitlesText: String = anime.alternativeTitles.synonyms.fold(enAndJaTitlesText) { acc, synonym ->
            if (acc.isNotBlank()) "$acc,\n$synonym" else synonym
        }
        binding.tvAlternativeTitlesBody.text = alternativeTitlesText

        binding.tvSynopsisBody.text = anime.synopsis
    }

    private fun bindAddToWatchlist(binding: AnimeDetailFragmentBinding, anime: Anime) {
        binding.vgAddToWatchlist.tvEpisodes.text = resources.getQuantityString(
            R.plurals.anime_detail_nb_episodes,
            anime.nbEpisodes,
            anime.nbEpisodes.toString()
        )
        binding.vgAddToWatchlist.root.setOnClickListener { viewModel.addToWatchlist() }
    }

    private fun bindWatchStatus(binding: AnimeDetailFragmentBinding, anime: Anime) {
        @StringRes val watchStatusTextResId: Int
        @DrawableRes val watchStatusDrawableResId: Int
        when (anime.myListStatus!!.status) {
            WatchStatus.COMPLETED -> {
                watchStatusTextResId = R.string.anime_detail_watch_status_completed
                watchStatusDrawableResId = R.drawable.ic_check_24_color_on_background
            }
            WatchStatus.DROPPED -> {
                watchStatusTextResId = R.string.anime_detail_watch_status_dropped
                watchStatusDrawableResId = R.drawable.ic_delete_24_color_on_background
            }
            WatchStatus.ON_HOLD -> {
                watchStatusTextResId = R.string.anime_detail_watch_status_on_hold
                watchStatusDrawableResId = R.drawable.ic_pause_24_color_on_background
            }
            WatchStatus.PLAN_TO_WATCH -> {
                watchStatusTextResId = R.string.anime_detail_watch_status_plan_to_watch
                watchStatusDrawableResId = R.drawable.ic_format_list_bulleted_24_color_on_background
            }
            WatchStatus.WATCHING -> {
                watchStatusTextResId = R.string.anime_detail_watch_status_watching
                watchStatusDrawableResId = R.drawable.ic_play_arrow_24_color_on_background
            }
        }
        binding.vgWatchStatus.tvWatchStatus.setText(watchStatusTextResId)
        binding.vgWatchStatus.tvWatchStatus.setCompoundDrawablesWithIntrinsicBounds(0, watchStatusDrawableResId, 0, 0)

        binding.vgWatchStatus.tvRating.text = anime.myListStatus!!.formatRating(requireContext())

        binding.vgWatchStatus.tvEpisodeProgress.text = getString(
            R.string.anime_detail_episodes_progress,
            anime.myListStatus!!.nbEpisodesWatched.toString(),
            anime.nbEpisodes.toString()
        )

        binding.vgWatchStatus.root.setOnClickListener {
            val bottomSheet: EditAnimeListStatusFragment = EditAnimeListStatusFragment.newInstance(anime)
            bottomSheet.callback = viewModel::updateListStatus
            bottomSheet.show(parentFragmentManager, null)
        }
    }

    private fun handleActionEvent(actionEvent: AnimeDetailActionEvent) {
        when (actionEvent) {
            AnimeDetailActionEvent.AddToWatchlistFailed -> Snackbar.make(
                requireView(),
                R.string.anime_detail_error_add_to_watchlist_failed,
                Snackbar.LENGTH_LONG
            ).show()
            AnimeDetailActionEvent.UpdateListStatusFailed -> Snackbar.make(
                requireView(),
                R.string.anime_detail_error_update_list_status_failed,
                Snackbar.LENGTH_LONG
            ).show()
        }.exhaustive
    }

    // endregion
}
