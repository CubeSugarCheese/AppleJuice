package moe.cugr.applejuice

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import moe.cugr.applejuice.ui.theme.AppleJuiceTheme

object BluetoothCallback : AdvertiseCallback() {
    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
        super.onStartSuccess(settingsInEffect)
        Log.v("AppleJuice", "onStartSuccess")
    }

    override fun onStartFailure(errorCode: Int) {
        Log.e("AppleJuice", "Advertising onStartFailure: $errorCode")
    }
}


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppleJuiceTheme {
                App()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun startAppleJuice(context: Context, permissionLauncher: ActivityResultLauncher<String>, bluetoothLauncher: ActivityResultLauncher<Intent>) {
    val bluetoothAdapter: BluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

    val bluetoothAllow = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
    val bluetoothAdvertiseAllow = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
    val bluetoothConnectAllow = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    if (!bluetoothAllow) {
        permissionLauncher.launch(Manifest.permission.BLUETOOTH)
    }
    if (!bluetoothAdvertiseAllow) {
        permissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
    }
    if (!bluetoothConnectAllow) {
        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (bluetoothAllow && bluetoothAdvertiseAllow && bluetoothConnectAllow) {
        if (bluetoothAdapter.isEnabled.not()) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothLauncher.launch(intent)
        } else {
            WorkManager.getInstance(context).beginUniqueWork("AppleJuiceWorker", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequest.Builder(AppleJuiceWorker::class.java).build()).enqueue()
        }
    }
}


@RequiresApi(Build.VERSION_CODES.S)
private fun stopAppleJuice(context: Context, launcher: ActivityResultLauncher<String>) {
    val bluetoothAllow = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
    val bluetoothAdvertiseAllow = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
    if (!bluetoothAllow) {
        launcher.launch(Manifest.permission.BLUETOOTH)
    }
    if (!bluetoothAdvertiseAllow) {
        launcher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
    }
    WorkManager.getInstance(context).cancelAllWork()
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun App() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            Log.i("Permission: ", "Granted")
        } else {
            Log.i("Permission: ", "Denied")
        }
    }
    val bluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            WorkManager.getInstance(context).beginUniqueWork("AppleJuiceWorker", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequest.Builder(AppleJuiceWorker::class.java).build()).enqueue()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var currentEnable by remember { mutableStateOf(false) }

        Button(onClick = {
            if (!currentEnable) {
                startAppleJuice(context, permissionLauncher, bluetoothLauncher)
            } else {
                stopAppleJuice(context, permissionLauncher)
            }
            currentEnable = !currentEnable
        }) {
            if (currentEnable) {
                Text(text = "Stop")
            } else {
                Text(text = "Start")
            }
        }
    }
}


