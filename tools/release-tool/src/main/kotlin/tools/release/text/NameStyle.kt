/*
 *  Copyright (c) 2022~2024 chr_56
 */

package tools.release.text

import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.BuiltArtifact
import com.android.build.api.variant.FilterConfiguration

sealed interface NameStyle {

    fun generateApkName(
        name: String,
        variant: ApplicationVariant,
        artifact: BuiltArtifact,
    ): String



    fun generateMappingName(
        name: String,
        variant: ApplicationVariant,
    ): String


    data object Version : NameStyle {
        override fun generateApkName(name: String, variant: ApplicationVariant, artifact: BuiltArtifact): String {
            val version = artifact.versionName ?: "NA"
            return "${name}_${version}"
        }

        override fun generateMappingName(name: String, variant: ApplicationVariant): String {
            return "mapping_${name}"
        }
    }

    data object VersionAbi : NameStyle {
        override fun generateApkName(name: String, variant: ApplicationVariant, artifact: BuiltArtifact): String {
            val version = artifact.versionName ?: "NA"
            val abiList =
                artifact.filters
                    .filter { it.filterType == FilterConfiguration.FilterType.ABI }
                    .takeIf { it.isNotEmpty() }
            val abi = abiList?.joinToString(separator = "-") ?: "universal"
            return "${name}_${version}_${abi}"
        }

        override fun generateMappingName(name: String, variant: ApplicationVariant): String {
            return "mapping_${name}"
        }
    }

    class VersionGitHashTime(val gitHash: String) : NameStyle {
        override fun generateApkName(name: String, variant: ApplicationVariant, artifact: BuiltArtifact): String {
            val version = artifact.versionName ?: "NA"
            return "${name}_${version}_${gitHash}_$currentTimeString"
        }

        override fun generateMappingName(name: String, variant: ApplicationVariant): String {
            return "mapping_${name}_${gitHash}"
        }
    }
}