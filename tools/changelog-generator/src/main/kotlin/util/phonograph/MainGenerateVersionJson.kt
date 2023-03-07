/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.parseReleaseNote
import util.phonograph.changelog.parseVersionJson
import util.phonograph.changelog.serializeVersionJson
import util.phonograph.changelog.updateVersionJson
import util.phonograph.changelog.writeToFile


fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]
    val outputPath = args[2]

    println("Parse data...")
    val model = parseReleaseNote("$rootPath/$sourcePath")
    val versionJson = parseVersionJson("$rootPath/$outputPath")

    println("Process version json")
    val newVersionJson = updateVersionJson(versionJson, model)
    val str = serializeVersionJson(newVersionJson)

    println("Output...")
    writeToFile(str, "$rootPath/$outputPath")

    println("Completed")
}