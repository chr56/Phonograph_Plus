/*
 * Copyright (c) 2022~2023 chr_56
 */

package tools.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class Plugin : Plugin<Project> {
    override fun apply(target: Project) {

    }

    companion object {
        const val PRODUCTS_DIR = "products"
        internal fun Project.productDir() = File(rootDir, PRODUCTS_DIR).also { it.mkdirs() }
    }
}