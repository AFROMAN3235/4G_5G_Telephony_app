package com.example.cellinfo.data.models

data class CellInfo(
    val technology: String,
    val isRegistered: Boolean,
    val signalStrength: SignalStrength,
    val cellIdentity: CellIdentity,
    val timestamp: Long = System.currentTimeMillis()
)
data class CellIdentity(
    val mcc: String?,
    val mnc: String?,
    val ci: Long?,
    val pci: Int?,
    val tac: Int?,
    val earfcn: Int?,
    val nrarfcn: Int?
)


data class SignalStrength(
    val dbm: Int,
    val asu: Int,
    val level: Int,

    
    val rsrp: Int?,
    val rsrq: Int?,
    val rssnr: Int?,


    val ssRsrp: Int?,
    val ssRsrq: Int?,
    val ssSinr: Int?,
    val csiRsrp: Int?,
    val csiRsrq: Int?
)