package com.agrosphere.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class TicketType(val label: String, val firestoreKey: String) {
    CHAT_SUPPORT("Chat",     "chat_support"),
    ISSUE       ("Issue",    "issue"),
    FEEDBACK    ("Feedback", "feedback"),
}

data class SupportTicket(
    val id: String,
    val type: TicketType,
    val message: String,
    val status: String,      // open | replied | resolved
    val devReply: String?,
    val createdAtMillis: Long,
)

object SupportTicketRepository {
    private val db   get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    suspend fun submit(type: TicketType, message: String): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext false
        try {
            db.collection("support_tickets").add(
                mapOf(
                    "userId"    to user.uid,
                    "userEmail" to (user.email ?: ""),
                    "type"      to type.firestoreKey,
                    "message"   to message,
                    "status"    to "open",
                    "source"    to "android",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun myTickets(): List<SupportTicket> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext emptyList()
        try {
            val snap = db.collection("support_tickets")
                .whereEqualTo("userId", user.uid)
                .limit(30)
                .get().await()
            snap.documents.mapNotNull { doc ->
                val key  = doc.getString("type") ?: return@mapNotNull null
                val type = TicketType.entries.firstOrNull { it.firestoreKey == key }
                    ?: TicketType.CHAT_SUPPORT
                SupportTicket(
                    id               = doc.id,
                    type             = type,
                    message          = doc.getString("message") ?: "",
                    status           = doc.getString("status")  ?: "open",
                    devReply         = doc.getString("devReply"),
                    createdAtMillis  = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                )
            }.sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) { emptyList() }
    }
}
