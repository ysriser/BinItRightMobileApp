package iss.nus.edu.sg.webviews.binitrightmobileapp

object AvatarAssetResolver {
    private val drawableMap = mapOf(
        "default_avatar" to R.drawable.default_avatar,
        "hoodie" to R.drawable.hoodie,
        "formal_suit" to R.drawable.formal_suit,
        "elegant_dress" to R.drawable.elegant_dress,
        "sports_attire" to R.drawable.sports_attire,
        "recycling_fan" to R.drawable.recycling_fan,
    )

    fun drawableForName(rawName: String): Int {
        val normalized = rawName.trim().lowercase().replace(Regex("\\s+"), "_")
        return drawableMap[normalized] ?: R.drawable.default_avatar
    }
}
