package mg.watched.data.anime

import androidx.lifecycle.asFlow
import androidx.paging.PagedList
import androidx.paging.toLiveData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mg.watched.core.utils.WResult
import mg.watched.data.anime.network.AnimeService
import mg.watched.data.anime.network.models.Anime
import mg.watched.data.anime.network.models.AnimeMoshiAdapters
import mg.watched.data.anime.network.models.MyListStatus
import mg.watched.data.anime.network.models.WatchStatus

class AnimeRepository(
    private val service: AnimeService,
) {

    val defaultAnimePagedListConfig = PagedList.Config.Builder()
        .setPageSize(10)
        .setEnablePlaceholders(false)
        .build()

    // region Animes

    private val animeDataSourceFactory: AnimeDataSourceFactory = AnimeDataSourceFactory(service)
    val animePagedListStream: Flow<PagedList<Anime>> = animeDataSourceFactory.toLiveData(defaultAnimePagedListConfig)
        .asFlow()

    fun createSearchDataSourceFactory(): AnimeSearchDataSourceFactory = AnimeSearchDataSourceFactory(service)

    // endregion

    // region Anime detail

    suspend fun addToWatchlist(animeId: Long): WResult<MyListStatus> = withContext(Dispatchers.IO) {
        try {
            val listStatus: MyListStatus =
                service.addToWatchList(animeId, AnimeMoshiAdapters().watchStatusToJson(WatchStatus.PLAN_TO_WATCH))
            WResult.Success(listStatus)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WResult.Failure(e)
        }
    }

    suspend fun updateListStatus(animeId: Long, listStatus: MyListStatus): WResult<MyListStatus> =
        withContext(Dispatchers.IO) {
            try {
                val updatedListStatus: MyListStatus = service.updateListStatus(
                    animeId,
                    listStatus.nbEpisodesWatched,
                    listStatus.score,
                    AnimeMoshiAdapters().watchStatusToJson(listStatus.status),
                )
                WResult.Success(updatedListStatus)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                WResult.Failure(e)
            }
        }

    // endregion
}
