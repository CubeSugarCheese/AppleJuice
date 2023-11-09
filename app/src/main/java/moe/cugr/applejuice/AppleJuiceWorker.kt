package moe.cugr.applejuice

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.lang.Thread.sleep

class AppleJuiceWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    private var stop: Boolean = false
    private var bluetoothAdapter: BluetoothAdapter = appContext.getSystemService(BluetoothManager::class.java).adapter
    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        while (true) {
            if (stop) {
                return Result.success()
            }

            for (manufacturerData in hex_data.shuffled()) {
                val manufacturerId = 0x004c

                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setConnectable(false)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .build()

                val data = AdvertiseData.Builder()
                    .addManufacturerData(manufacturerId, manufacturerData)
                    .build()

                bluetoothAdapter.bluetoothLeAdvertiser.startAdvertising(
                        settings,
                        data,
                        BluetoothCallback)
            }
            sleep(4*1000)
            bluetoothAdapter.bluetoothLeAdvertiser.stopAdvertising(BluetoothCallback)
        }
    }



    override fun onStopped() {
        this.stop = true
    }
}