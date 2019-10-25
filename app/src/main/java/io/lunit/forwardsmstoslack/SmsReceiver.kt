package io.lunit.forwardsmstoslack

import allbegray.slack.type.Payload
import allbegray.slack.webhook.SlackWebhookClient
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

import io.lunit.forwardsmstoslack.db.DBHelper


class SmsReceiver: BroadcastReceiver() {
    val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"
    val TAG = "SmsReceiver"
    # EDIT HERE.
    val DEFAULT_WEBHOOK_URL = "https://hooks.slack.com/services/default/incoming/webhook/url"

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.getAction().equals(SMS_RECEIVED)) {
            val bundle = intent?.getExtras()
            val messages = parseSmsMessage(bundle)
            if (messages.size > 0) {
                val sender = messages[0]?.getOriginatingAddress()
                val contents = messages[0]?.getMessageBody().toString()
                // val receivedDate = Date(messages[0]?.getTimestampMillis() as Long)


                # EDIT HERE.
                val SenderToChannelMap = mapOf("01012345678" to Pair("#channel", "https://hooks.slack.com/services/incoming/webhook/url"),
                                                "0212345678" to Pair("@someone", "https://hooks.slack.com/services/incoming/webhook/url"))

                val sender_str = sender.toString().replace("-", "")
                if (sender_str in SenderToChannelMap){
                    try {
                        val payload = Payload()
                        payload.text = ">*$sender*\n$contents".replace("\n", "\n>")
                        # EDIT HERE.
                        payload.username = "incoming-webhook"
                        payload.icon_emoji = ":slack:"
                        payload.channel = SenderToChannelMap[sender_str]?.first

                        object : AsyncTask<Void, Void, Void>() {
                            override fun doInBackground(vararg voids: Void): Void? {
                                // SlackWebhook API from https://github.com/pschroen/slack-api-android
                                var result = SlackWebhookClient(SenderToChannelMap[sender_str]?.second).post(payload)
                                Log.d(TAG, result)
                                return null
                            }
                        }.execute()
                    } catch (e : Exception){
                        Log.e(TAG, e.toString())
                        Log.e(TAG, sender)
                        Log.e(TAG, contents)

                        val payload = Payload()
                        payload.text = ">*$sender*\n$contents\n${e}".replace("\n", "\n>")
                        payload.username = "Slack Post Error"
                        payload.icon_emoji = ":exclamation:"
                        # EDIT HERE.
                        payload.channel = "@me"

                        object : AsyncTask<Void, Void, Void>() {
                            override fun doInBackground (vararg voids: Void): Void? {
                                SlackWebhookClient(DEFAULT_WEBHOOK_URL).post(payload)
                                return null
                            }
                        }.execute()
                    }
                }
                else {
                    Log.d(TAG, "Not registered Sender '$sender': $contents")
                }
            }
        }
    }

    private fun parseSmsMessage(bundle: Bundle?): Array<SmsMessage?>{
        val objects = bundle?.get("pdus") as Array<*>
        val messages = arrayOfNulls<SmsMessage>(objects.size)

        for (i in 0 until objects.size) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val format = bundle.getString("format")
                messages[i] = SmsMessage.createFromPdu(objects[i] as ByteArray, format)
            } else {
                messages[i] = SmsMessage.createFromPdu(objects[i] as ByteArray)
            }
        }

        return messages
    }
}