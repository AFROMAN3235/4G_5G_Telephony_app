package com.example.cellinfo.data.datasource

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.cellinfo.data.models.CellInfo
import com.example.cellinfo.data.models.NetworkState
import com.example.cellinfo.data.models.SignalStrength
import com.example.cellinfo.data.models.CellIdentity

class TelephonyDataSource(private val context: Context) {

    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    fun getNetworkState(): NetworkState {
        return if (hasPermissions()) {
            try {
                NetworkState(
                    operatorName = telephonyManager.networkOperatorName ?: "Unknown",
                    networkType = getNetworkTypeName(telephonyManager.dataNetworkType),
                    isRoaming = telephonyManager.isNetworkRoaming,
                    signalLevel = getSignalLevelDescription(telephonyManager.dataNetworkType)
                )
            } catch (e: SecurityException) {
                NetworkState("Permission required", "Unknown", false, "No access")
            }
        } else {
            NetworkState("Permission required", "Unknown", false, "No access")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getCellInfo(): List<CellInfo> {
        if (!hasPermissions()) {
            Log.w("TelephonyDataSource", "No permissions to access cell info")
            return emptyList()
        }

        return try {
            val allCellInfo = telephonyManager.allCellInfo
            allCellInfo?.mapNotNull { it.toCellInfo() } ?: emptyList()
        } catch (e: SecurityException) {
            Log.e("TelephonyDataSource", "Security exception: ${e.message}")
            emptyList()
        }
    }

    private fun hasPermissions(): Boolean {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getNetworkTypeName(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "Unknown"
        }
    }

    private fun getSignalLevelDescription(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE Signal"
            TelephonyManager.NETWORK_TYPE_NR -> "5G NR Signal"
            else -> "No 4G/5G"
        }
    }
}


@RequiresApi(Build.VERSION_CODES.Q)
private fun android.telephony.CellInfo.toCellInfo(): CellInfo? {
    return when (this) {
        is CellInfoLte -> createLteCellInfo(this)
        is CellInfoNr -> null
        else -> null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun createLteCellInfo(cellInfo: CellInfoLte): CellInfo {
    val signal = cellInfo.cellSignalStrength
    val identity = cellInfo.cellIdentity

    val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        identity.mccString
    } else {
        identity.mcc?.toString()
    }

    val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        identity.mncString
    } else {
        identity.mnc?.toString()
    }

    return CellInfo(
        technology = "LTE",
        isRegistered = cellInfo.isRegistered,
        signalStrength = SignalStrength(
            dbm = signal.dbm,
            asu = signal.asuLevel,
            level = signal.level,
            rsrp = signal.rsrp,
            rsrq = signal.rsrq,
            rssnr = signal.rssnr,
            ssRsrp = null, ssRsrq = null, ssSinr = null, csiRsrp = null, csiRsrq = null
        ),
        cellIdentity = CellIdentity(
            mcc = mcc, mnc = mnc,
            ci = identity.ci.takeIf { it != Int.MAX_VALUE }?.toLong(),
            pci = identity.pci.takeIf { it != Int.MAX_VALUE },
            tac = identity.tac.takeIf { it != Int.MAX_VALUE },
            earfcn = identity.earfcn.takeIf { it != Int.MAX_VALUE },
            nrarfcn = null
        )
    )
}

/*@RequiresApi(Build.VERSION_CODES.Q)
private fun createNrCellInfo(cellInfo: CellInfoNr): CellInfo {
    val signal = cellInfo.cellSignalStrength
    val identity = cellInfo.cellIdentity


    return CellInfo(
        technology = "NR",
        isRegistered = cellInfo.isRegistered,
        signalStrength = SignalStrength(
            dbm = signal.dbm,
            asu = signal.asuLevel,
            level = signal.level,
            rsrp = null, rsrq = null, rssnr = null,
            ssRsrp = getSsRsrp(signal),
            ssRsrq = getSsRsrq(signal),
            ssSinr = getSsSinr(signal),
            csiRsrp = getCsiRsrp(signal),
            csiRsrq = getCsiRsrq(signal)
        ),
        cellIdentity = CellIdentity(
            mcc = getMccFromIdentity(identity),
            mnc = getMncFromIdentity(identity),
            ci = getNciFromIdentity(identity),
            pci = getPciFromIdentity(identity),
            tac = getTacFromIdentity(identity),
            earfcn = null,
            nrarfcn = getNrarfcnFromIdentity(identity)
        )
    )
}
*/


@RequiresApi(Build.VERSION_CODES.Q)
private fun getNciFromIdentity(identity: android.telephony.CellIdentity): Long? {
    return try {
        (identity as? android.telephony.CellIdentityNr)?.nci?.takeIf { it != Long.MAX_VALUE }
    } catch (e: Exception) {
        null
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getPciFromIdentity(identity: android.telephony.CellIdentity): Int? {
    return try {
        (identity as? android.telephony.CellIdentityNr)?.pci?.takeIf { it != Int.MAX_VALUE }
    } catch (e: Exception) {
        null
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getTacFromIdentity(identity: android.telephony.CellIdentity): Int? {
    return try {
        (identity as? android.telephony.CellIdentityNr)?.tac?.takeIf { it != Int.MAX_VALUE }
    } catch (e: Exception) {
        null
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getNrarfcnFromIdentity(identity: android.telephony.CellIdentity): Int? {
    return try {
        (identity as? android.telephony.CellIdentityNr)?.nrarfcn?.takeIf { it != Int.MAX_VALUE }
    } catch (e: Exception) {
        null
    }
}


@RequiresApi(Build.VERSION_CODES.Q)
private fun getSsRsrp(signal: android.telephony.CellSignalStrength): Int? {
    return if (signal is android.telephony.CellSignalStrengthNr) {
        signal.ssRsrp.takeIf { it != Int.MAX_VALUE }
    } else null
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getSsRsrq(signal: android.telephony.CellSignalStrength): Int? {
    return if (signal is android.telephony.CellSignalStrengthNr) {
        signal.ssRsrq.takeIf { it != Int.MAX_VALUE }
    } else null
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getSsSinr(signal: android.telephony.CellSignalStrength): Int? {
    return if (signal is android.telephony.CellSignalStrengthNr) {
        signal.ssSinr.takeIf { it != Int.MAX_VALUE }
    } else null
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getCsiRsrp(signal: android.telephony.CellSignalStrength): Int? {
    return if (signal is android.telephony.CellSignalStrengthNr) {
        signal.csiRsrp.takeIf { it != Int.MAX_VALUE }
    } else null
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getCsiRsrq(signal: android.telephony.CellSignalStrength): Int? {
    return if (signal is android.telephony.CellSignalStrengthNr) {
        signal.csiRsrq.takeIf { it != Int.MAX_VALUE }
    } else null
}