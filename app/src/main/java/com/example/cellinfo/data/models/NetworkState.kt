package com.example.cellinfo.data.models

data class NetworkState(
    val operatorName: String,
    val networkType: String,
    val isRoaming: Boolean,
    val signalLevel: String
)