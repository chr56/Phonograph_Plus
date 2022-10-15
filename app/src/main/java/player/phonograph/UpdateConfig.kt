/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph

/**
 * urls of update source & mirrors
 */
object UpdateConfig {

    private const val owner = "chr56"
    private const val organization = "Phonograph-Plus"
    private const val repo = "Phonograph_Plus"
    private const val branch = "dev"
    private const val file = "version_catalog.json"

    const val requestUriGitHub = "https://raw.githubusercontent.com/$owner/$repo/$branch/$file"
    const val requestUriBitBucket = "https://bitbucket.org/$organization/$repo/raw/$branch/$file"

    const val requestUriJsdelivr = "https://cdn.jsdelivr.net/gh/$owner/$repo@$branch/$file"
    const val requestUriFastGit = "https://endpoint.fastgit.org/https://github.com/$owner/$repo/blob/$branch/$file"
}
