package com.cookiegames.smartcookie.search

import com.cookiegames.smartcookie.BrowserApp
import com.cookiegames.smartcookie.preference.PreferenceManager
import com.cookiegames.smartcookie.search.engine.*
import java.util.*
import javax.inject.Inject

/**
 * The model that provides the search engine based
 * on the user's preference.
 */
class SearchEngineProvider @Inject constructor() {

    @Inject internal lateinit var preferenceManager: PreferenceManager

    init {
        BrowserApp.getAppComponent().inject(this)
    }

    val currentSearchEngine: BaseSearchEngine
        get() {
            when (preferenceManager.searchChoice) {
                0 -> return CustomSearch(preferenceManager.searchUrl)
                1 -> return GoogleSearch()
                2 -> return AskSearch()
                3 -> return BingSearch()
                4 -> return YahooSearch()
                5 -> return StartPageSearch()
                6 -> return StartPageMobileSearch()
                7 -> return DuckSearch()
                8 -> return DuckLiteSearch()
                9 -> return BaiduSearch()
                10 -> return YandexSearch()
                else -> return GoogleSearch()
            }
        }

    fun mapSearchEngineToPreferenceIndex(searchEngine: BaseSearchEngine): Int {
        if (searchEngine is CustomSearch) {
            return 0
        } else if (searchEngine is GoogleSearch) {
            return 1
        } else if (searchEngine is AskSearch) {
            return 2
        } else if (searchEngine is BingSearch) {
            return 3
        } else if (searchEngine is YahooSearch) {
            return 4
        } else if (searchEngine is StartPageSearch) {
            return 5
        } else if (searchEngine is StartPageMobileSearch) {
            return 6
        } else if (searchEngine is DuckSearch) {
            return 7
        } else if (searchEngine is DuckLiteSearch) {
            return 8
        } else if (searchEngine is BaiduSearch) {
            return 9
        } else if (searchEngine is YandexSearch) {
            return 10
        } else {
            throw UnsupportedOperationException("Unknown search engine provided: " + searchEngine.javaClass)
        }
    }

    val allSearchEngines: List<BaseSearchEngine>
        get() = object : ArrayList<BaseSearchEngine>(11) {
            init {
                add(CustomSearch(preferenceManager.searchUrl))
                add(GoogleSearch())
                add(AskSearch())
                add(BingSearch())
                add(YahooSearch())
                add(StartPageSearch())
                add(StartPageMobileSearch())
                add(DuckSearch())
                add(DuckLiteSearch())
                add(BaiduSearch())
                add(YandexSearch())
            }
        }

}
