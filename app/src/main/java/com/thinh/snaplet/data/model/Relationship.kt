package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

data class Relationship(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("user1Id")
    val user1Id: String,
    
    @SerializedName("user2Id")
    val user2Id: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("initiator")
    val initiator: String,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String
)
