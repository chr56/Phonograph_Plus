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
            val variantOutputs = variant.outputs
            val version = variantOutputs.mapNotNull { it.versionName.orNull }.commonPrefix()
            return if (version == null) {
                "${name}_mapping"
            } else {
                "${name}_mapping_${version}"
            }
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
            val variantOutputs = variant.outputs
            val version = when (variantOutputs.size) {
                0    -> null
                1    -> variantOutputs.first().versionName.orNull
                else -> variantOutputs.mapNotNull { it.versionName.orNull }.toSet().joinToString("-")
            }
            return if (version == null) {
                "${name}_mapping"
            } else {
                "${name}_mapping_${version}"
            }
        }
    }

    class VersionGitHashTime(val gitHash: String) : NameStyle {
        override fun generateApkName(name: String, variant: ApplicationVariant, artifact: BuiltArtifact): String {
            val version = artifact.versionName ?: "NA"
            return "${name}_${version}_${gitHash}_$currentTimeString"
        }

        override fun generateMappingName(name: String, variant: ApplicationVariant): String {
            val version = variant.outputs.mapNotNull { it.versionName.orNull }.commonPrefix()
            return if (version == null) {
                "${name}_mapping_${gitHash}"
            } else {
                "${name}_mapping_${version}_${gitHash}"
            }
        }
    }
}

private fun Collection<String>.commonPrefix(): String? {
    if (isEmpty()) return null
    if (size == 1) return first()

    var index = -1
    while (true) {
        index++
        val chars = mapNotNull { it.getOrNull(index) }.takeIf { it.size == this.size }?.toSet()
        if (chars != null && chars.size == this.size) {
            continue
        } else {
            index--
            break
        }
    }

    return if (index >= 0) first().substring(index) else null
}