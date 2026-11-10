package com.wpf.utils.jiagu

import com.android.zipflinger.BytesSource
import com.android.zipflinger.ZipArchive
import java.io.*
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.security.*
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.zip.ZipFile

/** Independent content signature; APK can subsequently be signed with any deployment certificate. */
object IntegritySeal {
    @Synchronized
    fun key(directory: File): KeyPair {
        directory.mkdirs()
        FileChannel.open(File(directory, "key.lock").toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            channel.lock().use {
                val file = File(directory, "private.pk8")
                val factory = KeyFactory.getInstance("RSA")
                val privateKey = if (file.isFile) factory.generatePrivate(PKCS8EncodedKeySpec(file.readBytes())) else {
                    val generated = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().private
                    val staged = File.createTempFile("private-", ".tmp", directory)
                    try { staged.writeBytes(generated.encoded); XapkArchive.publish(staged, file) } finally { staged.delete() }
                    generated
                }
                val rsa = privateKey as RSAPrivateCrtKey
                return KeyPair(factory.generatePublic(RSAPublicKeySpec(rsa.modulus, rsa.publicExponent)), privateKey)
            }
        }
    }

    fun publicHex(key: KeyPair) = key.public.encoded.joinToString("") { "%02x".format(it.toInt() and 255) }

    fun seal(apk: File, key: KeyPair) {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            ZipFile(apk).use { zip ->
                val names = zip.entries().asSequence().filter { !it.isDirectory }.map { it.name }.filter {
                    it == "classes.dex" || it == "AndroidManifest.xml" || ResourceProtection.isResource(it) || NativeProtection.isLibrary(it)
                }.sorted().toList()
                require(names.distinct().size == names.size && "classes.dex" in names && "AndroidManifest.xml" in names && names.size in 3..100000)
                output.writeInt(0x4a474931); output.writeInt(names.size)
                names.forEach { name ->
                    val hash = MessageDigest.getInstance("SHA-256")
                    zip.getInputStream(zip.getEntry(name)).use { stream ->
                        val buffer = ByteArray(65536)
                        while (true) { val count = stream.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
                    }
                    output.writeUTF(name); output.write(hash.digest())
                }
            }
        }
        val record = bytes.toByteArray()
        require(record.size <= 32 * 1024 * 1024) { "资源完整性记录超过 32 MiB" }
        val signature = Signature.getInstance("SHA256withRSA").apply { initSign(key.private); update(record) }.sign()
        ZipArchive(apk.toPath()).use {
            it.add(BytesSource(record, "assets/jiagu-integrity.bin", 0))
            it.add(BytesSource(signature, "assets/jiagu-integrity.sig", 0))
        }
    }
}
