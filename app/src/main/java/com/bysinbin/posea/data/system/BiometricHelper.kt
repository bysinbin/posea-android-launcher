package com.bysinbin.posea.data.system

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import java.util.concurrent.Executors

object BiometricHelper {

    fun isDeviceSecure(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isDeviceSecure == true
    }

    fun authenticate(
        activity: Activity,
        title: String = "Uygulama Kilidi",
        subtitle: String = "Devam etmek için parmak izinizi veya ekran şifrenizi kullanın",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km == null || !km.isDeviceSecure) {
            // Cihazda ekran kilidi ayarlanmamışsa doğrudan aç
            onSuccess()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cancellationSignal = CancellationSignal()
                val executor = Executors.newSingleThreadExecutor()

                val builder = BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    builder.setNegativeButton("İptal", executor) { _, _ ->
                        activity.runOnUiThread { onError("İptal edildi") }
                    }
                }

                val prompt = builder.build()
                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            activity.runOnUiThread { onSuccess() }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            activity.runOnUiThread {
                                if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                                    errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                                    onError(errString?.toString() ?: "Kimlik doğrulama başarısız")
                                }
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                // Herhangi bir istisna durumunda güvenlik açığı yerine kullanıcıya bildir
                onError("Biyometrik doğrulama başlatılamadı: ${e.message}")
            }
        } else {
            // Android 8 ve öncesi
            onSuccess()
        }
    }
}
