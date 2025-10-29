package com.example.cellinfo.data.datasource

import android.util.Log
import com.example.cellinfo.data.models.CellInfo
import com.example.cellinfo.data.models.NetworkState
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zeromq.ZMQ


object ServCon {
    private var context: ZMQ.Context? = null
    private var socket: ZMQ.Socket? = null
    private var isConnected = false

    private const val SERVER_IP = "localhost"
    private const val PORT = 5555
    private val gson = Gson()

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("ServCon", "Попытка подключиться к $SERVER_IP:$PORT")


            context = ZMQ.context(1)
            socket = context?.socket(ZMQ.REQ)

            socket?.setReceiveTimeOut(5000)
            socket?.setSendTimeOut(5000)

            socket?.connect("tcp://$SERVER_IP:$PORT")


            val testData = mapOf(
                "test" to true,
                "message" to "connection_test",
                "time" to System.currentTimeMillis()
            )
            val testJson = gson.toJson(testData)

            val testSent = socket?.send(testJson.toByteArray(), 0)
            if (testSent == true) {
                val response = socket?.recvStr(0)
                if (response != null) {
                    isConnected = true
                    Log.d("ServCon", "Подключение установлено. Ответ сервера: $response")
                    return@withContext true
                } else {
                    Log.e("ServCon", "Нет ответа от сервера")
                    isConnected = false
                    return@withContext false
                }
            } else {
                Log.e("ServCon", "Не удалось отправить тестовое сообщение")
                isConnected = false
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("ServCon", "Ошибка подключения: ${e.message}")
            isConnected = false
            disconnect()
            return@withContext false
        }
    }

    suspend fun sendData(cellInfo: List<CellInfo>, networkState: NetworkState?): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) {
            Log.w("ServCon", "Нет подключения")
            return@withContext false
        }

        try {
            val jsonData = createSimpleJson(cellInfo, networkState)
            val sent = socket?.send(jsonData.toByteArray(), 0)

            if (sent == true) {
                val response = socket?.recvStr(0)
                if (response != null) {
                    Log.d("ServCon", "Данные отправлены. Ответ сервера: $response")
                    return@withContext true
                } else {
                    Log.e("ServCon", "Нет ответа от сервера после отправки")
                    isConnected = false
                    return@withContext false
                }
            } else {
                Log.e("ServCon", "Ошибка отправки: не удалось отправить данные")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("ServCon", "Ошибка отправки: ${e.message}")
            isConnected = false
            return@withContext false
        }
    }

    private fun createSimpleJson(
        cellInfo: List<CellInfo>,
        networkState: NetworkState?
    ): String {
        val simpleCells = cellInfo.map { cell ->
            mapOf(
                "tech" to cell.technology,
                "isMain" to cell.isRegistered,
                "signal" to cell.signalStrength.dbm,
                "rsrp" to cell.signalStrength.rsrp,
                "rsrq" to cell.signalStrength.rsrq,
                "mcc" to cell.cellIdentity.mcc,
                "mnc" to cell.cellIdentity.mnc,
                "pci" to cell.cellIdentity.pci,
                "tac" to cell.cellIdentity.tac
            )
        }

        val simpleData = mapOf(
            "time" to System.currentTimeMillis(),
            "operator" to networkState?.operatorName,
            "networkType" to networkState?.networkType,
            "cellsCount" to cellInfo.size,
            "device" to "Android",
            "cells" to simpleCells
        )

        return gson.toJson(simpleData)
    }

    fun disconnect() {
        try {
            socket?.close()
            context?.close()
            isConnected = false
        } catch (e: Exception) {
            Log.e("ServCon", "Ошибка: ${e.message}")
        }
    }

    fun isConnected(): Boolean = isConnected
}