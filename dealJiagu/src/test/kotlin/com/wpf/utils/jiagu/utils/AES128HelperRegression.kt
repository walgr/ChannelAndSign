package com.wpf.utils.jiagu.utils

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun main() {
    val key = "0123456789ABCDEF"
    val iv = "FEDCBA9876543210"
    val original = ByteArray(777) { (it % 251).toByte() }
    // Explicit arguments must work even when UI/global state contains other values.
    check(key != AES128Helper.DEFAULT_SECRET_KEY && iv != AES128Helper.KEY_VI)
    val encrypted = AES128Helper.encrypt(original, 512, key, iv)
    check(encrypted.size == original.size + 16)
    check(encrypted.copyOfRange(528, encrypted.size).contentEquals(original.copyOfRange(512, original.size)))
    val decryptor = Cipher.getInstance("AES/CBC/PKCS5Padding")
    decryptor.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
        IvParameterSpec(iv.toByteArray(Charsets.UTF_8)))
    check(decryptor.doFinal(encrypted.copyOf(528)).contentEquals(original.copyOf(512)))
    check(runCatching { AES128Helper.encrypt(original, 512, "short", iv) }.isFailure)
    check(runCatching { AES128Helper.encrypt(original, 512, key, "short") }.isFailure)
    check(runCatching { AES128Helper.encrypt(original, original.size + 1, key, iv) }.isFailure)
    println("AES payload regression passed: explicit parameters, binary prefix, padding, suffix, validation")
}
