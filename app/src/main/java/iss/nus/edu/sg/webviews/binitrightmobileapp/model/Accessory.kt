package iss.nus.edu.sg.webviews.binitrightmobileapp.model

data class Accessory(
    val accessoriesId: Long,
    val name: String,
    val imageUrl: String,
    val requiredPoints: Int,
    val owned: Boolean = false,
    val equipped: Boolean = false
)