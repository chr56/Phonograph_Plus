/*
 *  Copyright (c) 2022~2024 chr_56
 */

package tools.release.file

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.security.MessageDigest

fun File.hash(algorithm: String): ByteArray = hash(HashAlgorithm.named(algorithm))

fun File.hash(algorithm: HashAlgorithm): ByteArray {
    val md = MessageDigest.getInstance(algorithm.algorithmName)
    inputStream().use {
        md.update(it.readAllBytes())
    }
    return md.digest()
}

fun File.hashValidationFile(algorithm: String): File = hashValidationFileImpl(this, HashAlgorithm.named(algorithm))

fun File.hashValidationFile(algorithm: HashAlgorithm): File = hashValidationFileImpl(this, algorithm)

@OptIn(ExperimentalStdlibApi::class)
private fun hashValidationFileImpl(file: File, algorithm: HashAlgorithm): File {
    require(file.exists()) { "File(${file.absolutePath}) is not available" }
    require(file.isFile) { "File(${file.absolutePath}) is not a file!" }
    val hashFileName = "${file.absolutePath}.${algorithm.fileExtension}"
    val hashFile = File(hashFileName).assureFile()
    FileOutputStream(hashFile).use { outputStream ->
        OutputStreamWriter(outputStream).use { writer ->
            writer.write(file.hash(algorithm).toHexString(HexFormat.Default))
            writer.write(' '.code)
            writer.write(file.name)
            writer.write('\n'.code)
        }
    }
    return hashFile
}

enum class HashAlgorithm(val algorithmName: String, val fileExtension: String) {
    MD2("MD2", "md2"),
    MD5("MD5", "md5"),
    SHA1("SHA-1", "sha1"),
    SHA224("SHA-224", "sha224"),
    SHA256("SHA-256", "sha256"),
    SHA384("SHA-384", "sha384"),
    SHA512_224("SHA-512/224", "sha512_224"),
    SHA512_256("SHA-512/256", "sha512_256"),
    SHA3_224("SHA3-224", "sha3_224"),
    SHA3_256("SHA3-256", "sha3_256"),
    SHA3_384("SHA3-384", "sha3_384"),
    SHA3_512("SHA3-512", "sha3_512"),
    ;

    companion object {
        fun named(string: String): HashAlgorithm {
            val uppercase = string.uppercase()
            val lowercase = string.lowercase()
            return HashAlgorithm.entries.firstOrNull { it.algorithmName == uppercase }
                ?: HashAlgorithm.entries.firstOrNull { it.fileExtension == lowercase }
                ?: HashAlgorithm.valueOf(uppercase)
        }
    }
}