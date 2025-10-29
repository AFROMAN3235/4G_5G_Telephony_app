package com.example.cellinfo.data.datasource

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellSignalStrengthLte
import android.telephony.TelephonyManager
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
                val operatorName = telephonyManager.networkOperatorName ?: "Unknown"
                val networkType = telephonyManager.dataNetworkType
                val isRoaming = telephonyManager.isNetworkRoaming

                Log.d("TelephonyDataSource", "данные сети: Оператор=$operatorName, Тип=$networkType, Роуминг=$isRoaming")

                NetworkState(
                    operatorName = operatorName,
                    networkType = getNetworkTypeName(networkType),
                    isRoaming = isRoaming,
                    signalLevel = getSignalLevelDescription(networkType)
                )
            } catch (e: SecurityException) {
                Log.e("TelephonyDataSource", "SecurityException при получении состояния сети: " + e.message)
                NetworkState("Permission required", "Unknown", false, "No access")
            }
        } else {
            Log.w("TelephonyDataSource", "Нет разрешений для получения состояния сети")
            NetworkState("Permission required", "Unknown", false, "No access")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getRealCellInfoFromSignal(): List<CellInfo> {
        if (!hasPermissions()) {
            Log.w("TelephonyDataSource", "Нет разрешений для получения данных сигнала")
            return emptyList()
        }

        return try {
            val signalStrength = telephonyManager.signalStrength

            if (signalStrength == null) {
                Log.w("TelephonyDataSource", "SignalStrength недоступен")
                return emptyList()
            }

            val cellInfos = mutableListOf<CellInfo>()

            signalStrength.cellSignalStrengths.forEach { signal ->
                if (signal is CellSignalStrengthLte) {
                    Log.d("TelephonyDataSource", "Обнаружен LTE сигнал: dbm=" + signal.dbm +
                            ", level=" + signal.level +
                            ", rsrp=" + signal.rsrp +
                            ", rsrq=" + signal.rsrq +
                            ", rssnr=" + signal.rssnr)

                    val realCellInfo = createRealCellInfoFromLteSignal(signal)
                    cellInfos.add(realCellInfo)
                }
            }

            if (cellInfos.isEmpty()) {
                Log.d("TelephonyDataSource", "LTE сигналы не обнаружены")
            } else {
                Log.d("TelephonyDataSource", "Обработано LTE сигналов: " + cellInfos.size)
            }

            cellInfos

        } catch (e: SecurityException) {
            Log.e("TelephonyDataSource", "SecurityException при получении сигнала: " + e.message)
            emptyList()
        } catch (e: Exception) {
            Log.e("TelephonyDataSource", "Ошибка получения данных сигнала: " + e.message)
            emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createRealCellInfoFromLteSignal(signal: CellSignalStrengthLte): CellInfo {
        val operator = telephonyManager.simOperator ?: ""
        val mcc = if (operator.length >= 3) operator.substring(0, 3) else ""
        val mnc = if (operator.length > 3) operator.substring(3) else ""

        Log.d("TelephonyDataSource", "CellInfo: MCC=$mcc, MNC=$mnc, dbm=" + signal.dbm)

        return CellInfo(
            technology = "LTE",
            isRegistered = true,
            signalStrength = SignalStrength(
                dbm = signal.dbm,
                asu = signal.asuLevel,
                level = signal.level,
                rsrp = signal.rsrp,
                rsrq = signal.rsrq,
                rssnr = signal.rssnr,
                ssRsrp = null,
                ssRsrq = null,
                ssSinr = null,
                csiRsrp = null,
                csiRsrq = null
            ),
            cellIdentity = CellIdentity(
                mcc = mcc,
                mnc = mnc,
                ci = null,
                pci = null,
                tac = null,
                earfcn = null,
                nrarfcn = null
            )
        )
    }

    private fun hasPermissions(): Boolean {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        val allGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        return allGranted
    }

    private fun getNetworkTypeName(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            else -> "Unknown (" + networkType + ")"
        }
    }

    private fun getSignalLevelDescription(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE Signal"
            else -> "Unknown Signal"
        }
    }
}