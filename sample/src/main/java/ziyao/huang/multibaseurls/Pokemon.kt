package ziyao.huang.multibaseurls

/** Only the fields the demo needs from PokeApi's /pokemon/ditto response (Gson ignores undeclared fields). */
data class Pokemon(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
)
