package com.example.cellinfo.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cellinfo.data.datasource.TelephonyDataSource
import com.example.cellinfo.data.datasource.ServCon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val telephonyDataSource = TelephonyDataSource(application.applicationContext)

    var networkState: String = "Неизвестно"
    var cellCount: Int = 0
    var isConnected: Boolean = false
    var isLoading: Boolean = false
    var lastError: String? = null

    init {
        Log.d("MyApp", "ViewModel")
        connectToServer()
        startAutoRefresh()
    }

    private fun connectToServer() {
        viewModelScope.launch {
            isLoading = true
            lastError = null

            try {
                isConnected = ServCon.connect()
                Log.d("MyApp", "Подключение к серверу: $isConnected")

                if (!isConnected) {
                    lastError = "Не удалось подключиться к серверу"
                }
            } catch (e: Exception) {
                Log.e("MyApp", "Ошибка подключения: ${e.message}")
                lastError = "Ошибка подключения: ${e.message}"
                isConnected = false
            } finally {
                isLoading = false
            }
        }
    }

    fun refreshData() {
        if (isLoading) return

        isLoading = true
        lastError = null

        viewModelScope.launch {
            try {
                Log.d("MyApp", "обновление данных...")

                val state = telephonyDataSource.getNetworkState()
                networkState = "${state.operatorName} - ${state.networkType}"
                Log.d("MyApp", "Состояние сети: $networkState")

                val cells = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val cellsList = telephonyDataSource.getRealCellInfoFromSignal()
                    Log.d("MyApp", "Получено сот: ${cellsList.size}")
                    cellsList
                } else {
                    Log.d("MyApp", "Android версия < 10, CellInfo недоступен")
                    emptyList()
                }
                cellCount = cells.size

                if (cells.isEmpty()) {
                    Log.w("MyApp", "Не найдено ни одной соты!")
                } else {
                    Log.d("MyApp", "Найдено $cellCount сот")

                    if (isConnected) {
                        try {
                            val success = ServCon.sendData(cells, state)
                            Log.d("MyApp", if (success) "Данные отправлены на сервер" else "Ошибка отправки на сервер")
                        } catch (e: Exception) {
                            Log.e("MyApp", "Ошибка отправки на сервер: ${e.message}")
                        }
                    }
                }

            } catch (e: Exception) {
                lastError = "Ошибка: ${e.message}"
                Log.e("MyApp", "Ошибка обновления: ${e.message}")
            } finally {
                isLoading = false
                Log.d("MyApp", "Обновление завершено")
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            delay(3000)
            while (true) {
                try {
                    refreshData()
                    delay(10000)
                } catch (e: Exception) {
                    Log.e("MyApp", "Ошибка в авто-обновлении: ${e.message}")
                    delay(5000)
                }
            }
        }
    }

    fun sendToServer() {
        if (!isConnected) {
            lastError = "Нет подключения к серверу"
            return
        }

        viewModelScope.launch {
            try {
                isLoading = true
                val state = telephonyDataSource.getNetworkState()
                val cells = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    telephonyDataSource.getRealCellInfoFromSignal()
                } else {
                    emptyList()
                }

                if (cells.isNotEmpty()) {
                    val success = ServCon.sendData(cells, state)
                    if (success) {
                        Log.d("MyApp", "Данные отправлены")
                    } else {
                        lastError = "Не удалось отправить данные"
                        Log.w("MyApp", "Не отправлено")
                    }
                } else {
                    lastError = "Нет данных для отправки"
                    Log.w("MyApp", "Нет данных для отправки")
                }
            } catch (e: Exception) {
                lastError = "Ошибка отправки: ${e.message}"
                Log.e("MyApp", "Ошибка отправки: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            ServCon.disconnect()
            Log.d("MyApp", "TCP соединение закрыто")
        } catch (e: Exception) {
            Log.e("MyApp", "Ошибка при отключении: ${e.message}")
        }
        Log.d("MyApp", "ViewModel уничтожен")
    }
}