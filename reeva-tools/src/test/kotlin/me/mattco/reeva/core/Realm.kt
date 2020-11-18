package me.mattco.reeva.core

import me.mattco.reeva.runtime.primitives.JSSymbol

class Realm {
    companion object {
        val `@@toStringTag` = JSSymbol("toStringTag")
        val `@@species` = JSSymbol("species")
    }
}
