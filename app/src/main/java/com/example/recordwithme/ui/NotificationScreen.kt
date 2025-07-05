package com.example.recordwithme.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val CHANNEL_ID = "friend_request_channel"
private const val NOTIFICATION_ID = 1001

// 알림 팝업 함수
fun showNotification(context: Context, title: String, message: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Friend Requests",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "친구 신청 알림 채널"
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    with(NotificationManagerCompat.from(context)) {
        notify(NOTIFICATION_ID, builder.build())
    }
}

@Composable
fun NotificationScreen(
    context: Context = LocalContext.current,
    currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
) {
    val firestore = FirebaseFirestore.getInstance()
    val realtime = FirebaseDatabase.getInstance()
    var requests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }

    // 실시간 리스너 등록
    LaunchedEffect(currentUserId) {
        val ref = realtime.getReference("friendRequests/$currentUserId")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val fromUserId = snapshot.key ?: return
                val fromUserName = snapshot.child("fromUserName").getValue(String::class.java) ?: "알 수 없음"

                showNotification(context, "새 친구 신청", "$fromUserName 님이 친구 신청을 보냈습니다.")

                // Firestore에서 요청 가져오기
                firestore.collection("users")
                    .document(currentUserId)
                    .collection("friendRequests")
                    .get()
                    .addOnSuccessListener { result ->
                        requests = result.documents.mapNotNull { doc ->
                            val id = doc.getString("fromUserId") ?: return@mapNotNull null
                            val name = doc.getString("fromUserName") ?: "알 수 없음"
                            FriendRequest(id, name)
                        }
                    }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("친구 신청 알림", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (requests.isEmpty()) {
            Text("알림이 없습니다.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        } else {
            requests.forEach { request ->
                FriendRequestItem(
                    request = request,
                    onAccept = {
                        acceptFriendRequest(currentUserId, request.fromUserId, request.fromUserName)
                        requests = requests.filter { it.fromUserId != request.fromUserId }
                    },
                    onReject = {
                        denyFriendRequest(currentUserId, request.fromUserId)
                        requests = requests.filter { it.fromUserId != request.fromUserId }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${request.fromUserName} 님이 친구 신청을 보냈습니다.")
            }
            Row {
                TextButton(onClick = onAccept) { Text("승인") }
                TextButton(onClick = onReject) { Text("거절") }
            }
        }
    }
}

data class FriendRequest(val fromUserId: String, val fromUserName: String)

fun acceptFriendRequest(currentUserId: String, fromUserId: String, fromUserName: String) {
    val db = FirebaseFirestore.getInstance()
    val batch = db.batch()

    val myRef = db.collection("users").document(currentUserId)
        .collection("friends").document(fromUserId)
    batch.set(myRef, mapOf("name" to fromUserName))

    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: currentUserId
    val theirRef = db.collection("users").document(fromUserId)
        .collection("friends").document(currentUserId)
    batch.set(theirRef, mapOf("name" to currentUserEmail))

    val requestRef = db.collection("users")
        .document(currentUserId)
        .collection("friendRequests").document(fromUserId)
    batch.delete(requestRef)

    batch.commit()

    // Realtime 알림 제거
    FirebaseDatabase.getInstance()
        .getReference("friendRequests/$currentUserId/$fromUserId")
        .removeValue()
}

fun denyFriendRequest(currentUserId: String, fromUserId: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(currentUserId)
        .collection("friendRequests").document(fromUserId)
        .delete()

    FirebaseDatabase.getInstance()
        .getReference("friendRequests/$currentUserId/$fromUserId")
        .removeValue()
}