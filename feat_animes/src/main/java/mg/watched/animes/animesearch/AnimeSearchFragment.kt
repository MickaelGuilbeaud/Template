package mg.watched.animes.animesearch

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.android.synthetic.main.fragment_anime_search.*
import mg.watched.animes.R
import mg.watched.animes.animedetail.AnimeDetailFragment
import mg.watched.animes.animes.AnimeAdapter
import mg.watched.animes.utils.AnimeAnimations
import mg.watched.core.base.BaseFragment
import mg.watched.core.requireFragmentContainerProvider
import mg.watched.core.utils.exhaustive
import mg.watched.core.utils.getWindowBackgroundColor
import mg.watched.core.utils.hideKeyboard
import mg.watched.core.utils.toPx
import mg.watched.design.MarginItemDecoration
import org.koin.androidx.viewmodel.ext.android.viewModel

class AnimeSearchFragment : BaseFragment(R.layout.fragment_anime_search) {

    companion object {
        fun newInstance(): AnimeSearchFragment = AnimeSearchFragment()
    }

    // region Properties

    private val viewModel: AnimeSearchViewModel by viewModel()

    private val animeAdapter = AnimeAdapter { anime, view ->
        val fragment = AnimeDetailFragment.newInstance(anime)
        parentFragmentManager.commit {
            addSharedElement(view, AnimeAnimations.getAnimeMasterDetailTransitionName(anime))
            addToBackStack(null)
            replace(requireFragmentContainerProvider().getFragmentContainerId(), fragment)
        }
    }

    // endregion

    // region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            containerColor = requireContext().getWindowBackgroundColor()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()

        viewModel.viewStates().observe(viewLifecycleOwner) { bindViewState(it) }
    }

    private fun initUI() {
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }

        etSearch.doOnTextChanged { text, _, _, _ -> viewModel.searchAnimes(text.toString().trim()) }

        rvAnimes.setHasFixedSize(true)
        rvAnimes.adapter = animeAdapter
        rvAnimes.addItemDecoration(MarginItemDecoration(12.toPx(requireContext())))
        rvAnimes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                hideKeyboard()
            }
        })
    }

    // endregion

    // region ViewStates, NavigationEvents and ActionEvents

    private fun bindViewState(viewState: AnimeSearchViewState) {
        when (viewState) {
            AnimeSearchViewState.NoSearch -> {
                tvNoSearch.isVisible = true
                pbLoading.isVisible = false
                tvNoSearchResult.isVisible = false
                rvAnimes.isVisible = false
            }
            AnimeSearchViewState.Loading -> {
                tvNoSearch.isVisible = false
                pbLoading.isVisible = true
                tvNoSearchResult.isVisible = false
                rvAnimes.isVisible = false
            }
            AnimeSearchViewState.NoSearchResult -> {
                tvNoSearch.isVisible = false
                pbLoading.isVisible = false
                tvNoSearchResult.isVisible = true
                rvAnimes.isVisible = false
            }
            is AnimeSearchViewState.SearchResults -> {
                tvNoSearch.isVisible = false
                pbLoading.isVisible = false
                tvNoSearchResult.isVisible = false
                rvAnimes.isVisible = true

                animeAdapter.submitList(viewState.animes)
            }
        }.exhaustive
    }

    // endregion
}