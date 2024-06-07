package com.learn.antivol

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    private val TAG = "RecepteurAdminAppareil"
    private val mailSender = MailSender("votre_email@gmail.com", "votre_mot_de_passe")

    private fun afficherToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "onEnabled")
        afficherToast(context, "Administrateur de l'appareil activé")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Désactivation de l'administrateur de l'appareil demandée"

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "onDisabled")
        afficherToast(context, "Administrateur de l'appareil désactivé")
    }

    override fun onPasswordChanged(context: Context, intent: Intent, userHandle: UserHandle) {
        afficherToast(context, "Mot de passe modifié")
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: UserHandle) {
        Log.i(TAG, "onPasswordSucceeded")
        afficherToast(context, "Mot de passe réussi")
    }

    @Deprecated("Deprecated in Java")
    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        val sharedPref = context.getSharedPreferences("ECHECS_MOT_DE_PASSE", Context.MODE_PRIVATE)
        val echecsMotDePasse = sharedPref.getInt("tentatives", 0) + 1
        sharedPref.edit().putInt("tentatives", echecsMotDePasse).apply()

        if (echecsMotDePasse == 1) {
            Log.w(TAG, "Échec de tentative de mot de passe")
            sharedPref.edit().putInt("tentatives", 0).apply()

            // Envoyer un e-mail en cas d'échec de saisie du code PIN
            GlobalScope.launch {
                mailSender.sendMail(
                    "Échec de tentative de mot de passe",
                    "Il y a eu une tentative de saisie incorrecte du code PIN.",
                    listOf("destinataire@example.com"),
                    "chemin/vers/fichier/attaché"
                )
            }
        }
    }
}
