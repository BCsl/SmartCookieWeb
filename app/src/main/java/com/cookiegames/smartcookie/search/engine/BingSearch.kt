package com.cookiegames.smartcookie.search.engine

import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.constant.Constants

/**
 * The Bing search engine.
 *
 * See http://upload.wikimedia.org/wikipedia/commons/thumb/b/b1/Bing_logo_%282013%29.svg/500px-Bing_logo_%282013%29.svg.png
 * for the icon.
 */
class BingSearch : BaseSearchEngine(
        "file:///android_asset/bing.png",
        Constants.BING_SEARCH,
        R.string.search_engine_bing
)
