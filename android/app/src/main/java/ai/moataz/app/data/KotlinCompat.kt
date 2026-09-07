package ai.moataz.app.data

/** Kotlin stdlib does not provide orEmpty() for nullable ByteArray on all toolchains. */
fun ByteArray?.orEmpty(): ByteArray = this ?: ByteArray(0)
