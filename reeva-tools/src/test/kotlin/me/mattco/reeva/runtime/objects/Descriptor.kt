package me.mattco.reeva.runtime.objects

import me.mattco.reeva.runtime.JSValue

data class Descriptor constructor(
    private var value: JSValue,
    var attributes: Int,
) {
    companion object {
        const val CONFIGURABLE = 1 shl 0
        const val ENUMERABLE = 1 shl 1
        const val WRITABLE = 1 shl 2
        const val HAS_CONFIGURABLE = 1 shl 3
        const val HAS_ENUMERABLE = 1 shl 4
        const val HAS_WRITABLE = 1 shl 5
        const val HAS_BASIC = HAS_CONFIGURABLE or HAS_ENUMERABLE or HAS_WRITABLE

        const val defaultAttributes = CONFIGURABLE or ENUMERABLE or WRITABLE or HAS_BASIC
    }
}
