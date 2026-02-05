package iss.nus.edu.sg.webviews.binitrightmobileapp.model

data class UserAccessory(
    val userAccessoriesId: Long,
    val equipped: Boolean,
    val accessories: Accessory // This is the nested object containing the name
)