/*package com.google.mediapipe.examples.llminference

import android.util.Log
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetUpdates
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse

/**
 * Uses Pengrad's TelegramBot and manual long polling.
 */
class MyPhoneBot(
    private val botToken: String,
    private val botUsername: String
) {
    // Pengrad's bot instance
    private val bot = TelegramBot(botToken)

    fun startLongPolling() {
        Thread {
            try {
                println("MyPhoneBot: Starting the polling thread...")
                var offset = 0
                while (true) {
                    println("MyPhoneBot: Polling iteration, offset=$offset")
                    val updatesResponse = bot.execute(
                        GetUpdates().offset(offset).limit(100).timeout(30)
                    )
                    val updates: List<Update>? = updatesResponse.updates()
                    println("MyPhoneBot: Polled Telegram, got ${updates?.size ?: 0} updates")

                    updates?.forEach { update ->
                        offset = update.updateId() + 1
                        onUpdateReceived(update)
                    }

                    Thread.sleep(1000)
                }
            } catch (e: Exception) {
                println("❌ Exception in MyPhoneBot thread: ${e.message}")
                println(e.stackTraceToString()) // This will print the full trace to Logcat
            }
        }.start()
    }


    private fun onUpdateReceived(update: Update) {
        val message = update.message() ?: return
        val text = message.text() ?: return

        val chatId = message.chat().id()
        println("PhoneBot => Chat ID is: $chatId")



        if (text.isNotBlank()) {
            val chatId = message.chat().id().toString()

            if (text.trim().startsWith("{") && text.contains("\"chunks\"")) {
                // Chunk assignment JSON
                handleChunkAssignment(text, chatId)
            } else if (text.trim().startsWith("{") && text.contains("\"queryId\"")) {
                // Query JSON
                handleIncomingQuery(text, chatId)
            } else {
                // Some other message
                sendTextMessage(chatId, "Received unknown message: $text")
            }
        }
    }

    private fun handleChunkAssignment(jsonString: String, chatId: String) {
        // 1) Parse JSON, store chunks locally
        PhoneTelegramHandler.handleChunkAssignment(jsonString)

        // 2) Optionally reply
        sendTextMessage(chatId, "Chunks stored on phone!")

        // 3) Print how many total chunks we have now
        val totalChunks = ChunkRepository.getAllChunks().size
        println("MyPhoneBot: handleChunkAssignment => now have $totalChunks total chunks stored.")
    }

    private fun handleIncomingQuery(jsonString: String, chatId: String) {
        val partialSummaryJson = PhoneTelegramHandler.handleQueryMessage(jsonString)
        sendTextMessage(chatId, partialSummaryJson)
    }

    fun sendTextMessage(chatId: String, text: String) {
        val chatIdLong = chatId.toLongOrNull() ?: return
        val request = SendMessage(chatIdLong, text)
        val response: SendResponse = bot.execute(request)
        if (!response.isOk) {
            println("Failed to send message: ${response.errorCode()} - ${response.description()}")
        }
    }
}*/
