package com.example.saogallery.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

/**
 * Password backup that does not depend on the device KeyStore key.
 * File layout is big-endian:
 * "NYTEBAK1" | version u8=1 | kdf u8=1 | iterations u32 | salt 16 | count u32
 * then each entry: iv 12 | cipherLen u32 | AES-256-GCM(payload).
 * Payload: id u16+utf8, name u16+utf8, takenAt i64, trashedAt i64 (0 = not trashed), image u32+bytes.
 */
object VaultBackup {
    const val ITERATIONS = 600_000
    private val MAGIC = "NYTEBAK1".toByteArray(Charsets.US_ASCII)
    private const val HEADER = 34
    private const val COUNT_AT = 30

    data class Entry(
        val id: String,
        val name: String,
        val takenAt: Long,
        val trashedAt: Long?,
        val bytes: ByteArray,
    )

    sealed class ReadResult {
        data class Ok(val added: Int) : ReadResult()
        data object BadPassword : ReadResult()
        data class Damaged(val added: Int) : ReadResult()
    }

    fun begin(password: String, file: RandomAccessFile): Writer {
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val key = derive(password, salt, ITERATIONS)
        file.setLength(0)
        file.write(MAGIC)
        file.write(1)
        file.write(1)
        file.writeInt(ITERATIONS)
        file.write(salt)
        file.writeInt(0)
        return Writer(file, key)
    }

    class Writer(private val file: RandomAccessFile, private val key: ByteArray) : AutoCloseable {
        private var count = 0
        private var closed = false

        fun add(entry: Entry) {
            val payload = payload(entry)
            val iv = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
            val body = encrypt(key, iv, payload)
            payload.fill(0)
            file.write(iv)
            file.writeInt(body.size)
            file.write(body)
            count++
        }

        override fun close() {
            if (closed) return
            closed = true
            try {
                file.seek(COUNT_AT.toLong())
                file.writeInt(count)
            } finally {
                key.fill(0)
            }
        }
    }

    fun restore(password: String, input: InputStream, onEntry: (Entry) -> Unit): ReadResult {
        val header = ByteArray(HEADER)
        if (!input.readExact(header)) return ReadResult.Damaged(0)
        if (!header.copyOfRange(0, 8).contentEquals(MAGIC) || header[8] != 1.toByte() || header[9] != 1.toByte()) {
            return ReadResult.Damaged(0)
        }
        val view = DataInputStream(ByteArrayInputStream(header))
        view.skipBytes(10)
        val iterations = view.readInt()
        if (iterations !in 1..5_000_000) return ReadResult.Damaged(0)
        val salt = ByteArray(16)
        view.readFully(salt)
        val count = view.readInt()
        if (count !in 0..100_000) return ReadResult.Damaged(0)
        val key = derive(password, salt, iterations)
        try {
            val data = DataInputStream(input)
            var added = 0
            repeat(count) { index ->
                val iv = ByteArray(12)
                if (!data.readExact(iv)) return if (added == 0) ReadResult.Damaged(0) else ReadResult.Damaged(added)
                val length = data.readInt()
                if (length !in 16..(80 * 1024 * 1024)) {
                    return if (index == 0) ReadResult.BadPassword else ReadResult.Damaged(added)
                }
                val body = ByteArray(length)
                if (!data.readExact(body)) return if (added == 0) ReadResult.Damaged(0) else ReadResult.Damaged(added)
                val plain = try {
                    decrypt(key, iv, body)
                } catch (_: Exception) {
                    return if (index == 0) ReadResult.BadPassword else ReadResult.Damaged(added)
                }
                val entry = try {
                    parse(plain)
                } catch (_: Exception) {
                    plain.fill(0)
                    return ReadResult.Damaged(added)
                }
                plain.fill(0)
                onEntry(entry)
                entry.bytes.fill(0)
                added++
            }
            return ReadResult.Ok(added)
        } finally {
            key.fill(0)
        }
    }

    private fun payload(entry: Entry): ByteArray {
        val id = entry.id.take(80).toByteArray(Charsets.UTF_8)
        val name = entry.name.take(180).toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { data ->
            data.writeShort(id.size)
            data.write(id)
            data.writeShort(name.size)
            data.write(name)
            data.writeLong(entry.takenAt)
            data.writeLong(entry.trashedAt ?: 0L)
            data.writeInt(entry.bytes.size)
            data.write(entry.bytes)
        }
        return out.toByteArray()
    }

    private fun parse(plain: ByteArray): Entry {
        val data = DataInputStream(ByteArrayInputStream(plain))
        val id = ByteArray(data.readUnsignedShort())
        data.readFully(id)
        val name = ByteArray(data.readUnsignedShort())
        data.readFully(name)
        val takenAt = data.readLong()
        val trashedAt = data.readLong().takeIf { it > 0L }
        val size = data.readInt()
        if (size < 0 || size > plain.size) error("image")
        val bytes = ByteArray(size)
        data.readFully(bytes)
        return Entry(id.toString(Charsets.UTF_8), name.toString(Charsets.UTF_8), takenAt, trashedAt, bytes)
    }

    private fun encrypt(key: ByteArray, iv: ByteArray, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(plain)
    }

    private fun decrypt(key: ByteArray, iv: ByteArray, body: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(body)
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        return pbkdf2(password.toByteArray(Charsets.UTF_8), salt, iterations, 32)
    }

    internal fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))
        val blocks = (length + 31) / 32
        val out = ByteArray(blocks * 32)
        var offset = 0
        for (block in 1..blocks) {
            mac.reset()
            mac.update(salt)
            mac.update(
                byteArrayOf(
                    (block ushr 24).toByte(),
                    (block ushr 16).toByte(),
                    (block ushr 8).toByte(),
                    block.toByte(),
                ),
            )
            var u = mac.doFinal()
            val t = u.copyOf()
            for (round in 2..iterations) {
                mac.reset()
                u = mac.doFinal(u)
                for (index in t.indices) t[index] = t[index] xor u[index]
            }
            t.copyInto(out, offset)
            offset += 32
        }
        return if (out.size == length) out else out.copyOf(length)
    }

    private fun RandomAccessFile.writeInt(value: Int) {
        write(
            byteArrayOf(
                (value ushr 24).toByte(),
                (value ushr 16).toByte(),
                (value ushr 8).toByte(),
                value.toByte(),
            ),
        )
    }

    private fun InputStream.readExact(buf: ByteArray): Boolean {
        var off = 0
        while (off < buf.size) {
            val n = read(buf, off, buf.size - off)
            if (n < 0) return false
            off += n
        }
        return true
    }
}
