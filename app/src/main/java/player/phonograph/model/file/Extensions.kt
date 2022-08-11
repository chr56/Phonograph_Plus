/*
 * Copyright (c) 2022 chr_56
 */
@file:JvmName("Util")
package player.phonograph.model.file

fun MutableList<FileEntity>.put(item: FileEntity) {
    when (item) {
        is FileEntity.File -> {
            this.add(item)
        }
        is FileEntity.Folder -> {
            // count songs for folder
            val i = this.indexOf(item)
            if (i < 0) {
                this.add(item.apply { songCount = 1 })
            } else {
                (this[i] as FileEntity.Folder).songCount ++
            }
        }
    }
}