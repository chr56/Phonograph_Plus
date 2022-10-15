/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph

/**
 * urls of update source & mirrors
 */
object UpdateConfig {

    private const val OWNER = "chr56"
    private const val ORGANIZATION = "Phonograph-Plus"
    private const val REPO = "Phonograph_Plus"
    private const val BRANCH = "dev"
    private const val FILE = "version_catalog.json"

    const val GITHUB_REPO = "$OWNER/$REPO"

    const val requestUriGitHub = "https://raw.githubusercontent.com/$GITHUB_REPO/$BRANCH/$FILE"
    const val requestUriBitBucket = "https://bitbucket.org/$ORGANIZATION/$REPO/raw/$BRANCH/$FILE"

    const val requestUriJsdelivr = "https://cdn.jsdelivr.net/gh/$GITHUB_REPO@$BRANCH/$FILE"
    const val requestUriFastGit = "https://endpoint.fastgit.org/https://github.com/$GITHUB_REPO/blob/$BRANCH/$FILE"
}
