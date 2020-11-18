package me.mattco.reeva.runtime.objects

import me.mattco.reeva.runtime.primitives.JSSymbol

class PropertyKey private constructor(val value: Any) {
    constructor(value: String) : this(value as Any)
    constructor(value: JSSymbol) : this(value as Any)
}
