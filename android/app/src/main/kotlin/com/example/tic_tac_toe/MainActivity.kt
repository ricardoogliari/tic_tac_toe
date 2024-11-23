package com.example.tic_tac_toe

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {

    private val CHANNEL = "game/exchange"
    private var pubnub: PubNub? = null
    private var channel: String? = null
    private var handler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = Handler(Looper.getMainLooper())

        val pnConfiguration = PNConfiguration("channel_tic_tac_toe")
        pnConfiguration.subscribeKey = "sub-c-02dea89f-e75a-4ba5-87bc-fdac744a5055"
        pnConfiguration.publishKey = "pub-c-c98aeda0-c03f-45db-9a89-52706e1be41c"
        pubnub = PubNub(pnConfiguration)

        pubnub?.let {
            it.addListener(object : SubscribeCallback() {
                override fun status(pubnub: PubNub, status: PNStatus) {}
                override fun message(pubnub: PubNub, message: PNMessageResult) {
                    val receivedMessageObject = message.message.asJsonObject["tap"]
                    Log.e("pubnub", "Received message content: $receivedMessageObject")

                    handler?.let {
                        it.post {
                            //invocando um método no DART.. Kotlin -> DART
                            flutterEngine?.dartExecutor?.binaryMessenger?.let { it1 ->
                                MethodChannel(it1, CHANNEL)
                                    .invokeMethod("sendAction", receivedMessageObject.asString)
                            };
                        }
                    }
                }
                override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {}
                override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}
                override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {}
                override fun channel(
                    pubnub: PubNub,
                    pnChannelMetadataResult: PNChannelMetadataResult
                ) {}
                override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {}
                override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
                override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult){}
            })
        }

    }


    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
                call, result ->
            if (call.method == "sendAction") {
                pubnub!!.publish()
                    .message(call.arguments)
                    .channel(channel)
                    .async { result, status ->
                        Log.e("pubnub", "teve erro? ${status.isError}")
                    }
                result.success(true)
            } else if (call.method == "subscribe") {
                subscribeChannel(call.argument("channel"))
                result.success(true)
            } else {
                result.notImplemented()
            }
        }
    }


    fun subscribeChannel(channelName: String?){
        channel = channelName
        channelName?.let {
            pubnub?.subscribe()?.channels(listOf(channelName))?.execute();
        }
    }

}



