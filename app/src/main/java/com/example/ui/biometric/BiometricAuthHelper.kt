package com.example.ui.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricHardwareStatus(val label: String, val isAvailable: Boolean) {
    READY("Biometric Ready (Fingerprint / Face)", true),
    NOT_ENROLLED("Biometrics Supported but not Enrolled", false),
    NO_HARDWARE("No Biometric Hardware Detected", false),
    UNAVAILABLE("Biometric Sensor Temporarily Unavailable", false),
    UNKNOWN("Status Unknown", false)
}

object BiometricAuthHelper {

    fun checkBiometricStatus(context: Context): BiometricHardwareStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricHardwareStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricHardwareStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricHardwareStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricHardwareStatus.UNAVAILABLE
            else -> BiometricHardwareStatus.UNKNOWN
        }
    }

    fun promptBiometricAuthentication(
        activity: FragmentActivity,
        employeeName: String,
        actionTitle: String = "Mark Attendance",
        onSuccess: (authTag: String) -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit,
        onFallbackRequested: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val authType = when (result.authenticationType) {
                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> "BIOMETRIC_AUTHENTICATED"
                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL -> "DEVICE_CREDENTIAL_AUTHENTICATED"
                    else -> "BIOMETRIC_VERIFIED"
                }
                onSuccess(authType)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    onFallbackRequested()
                } else {
                    onError(errorCode, errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError(-1, "Biometric recognition unconfirmed. Please try placing your finger again.")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("$actionTitle: $employeeName")
            .setSubtitle("Verify identity via Fingerprint or Face recognition")
            .setDescription("App Boss requires authenticated identity before recording present attendance.")
            .setNegativeButtonText("Use PIN / Password")
            .setConfirmationRequired(true)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}
