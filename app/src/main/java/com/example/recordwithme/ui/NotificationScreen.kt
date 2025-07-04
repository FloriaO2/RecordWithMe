package com.example.recordwithme.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// 알림 데이터 클래스
data class Notification(
    val id: String,
    val type: String, // "friend_request", "friend_accepted", "friend_rejected"
    val fromUserId: String,
    val fromUserName: String,
    val timestamp: Long,
    val isRead: Boolean = false
) {
    val formattedTime: String
        get() {
            val date = Date(timestamp)
            val now = Date()
            val diff = now.time - timestamp
            
            return when {
                diff < 60000 -> "방금 전" // 1분 미만
                diff < 3600000 -> "${diff / 60000}분 전" // 1시간 미만
                diff < 86400000 -> "${diff / 3600000}시간 전" // 1일 미만
                else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
            }
        }
}

// 친구 요청 아이템 컴포저블
@Composable
fun FriendRequestItem(
    notification: Notification,
    onAccept: (Notification) -> Unit,
    onReject: (Notification) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 프로필 이미지 (이니셜)
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFBDBDBD)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notification.fromUserName.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.fromUserName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "친구 요청을 보냈습니다",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = notification.formattedTime,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 수락/거절 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAccept(notification) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Accept",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("수락")
                }
                
                OutlinedButton(
                    onClick = { onReject(notification) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Reject",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("거절")
                }
            }
        }
    }
}

// 일반 알림 아이템 컴포저블
@Composable
fun GeneralNotificationItem(
    notification: Notification
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 알림 타입에 따른 아이콘
            val icon = when (notification.type) {
                "friend_accepted" -> Icons.Filled.CheckCircle
                "friend_rejected" -> Icons.Filled.Cancel
                else -> Icons.Filled.Notifications
            }
            
            val iconColor = when (notification.type) {
                "friend_accepted" -> Color(0xFF4CAF50)
                "friend_rejected" -> Color(0xFFF44336)
                else -> Color(0xFF2196F3)
            }
            
            Icon(
                icon,
                contentDescription = "Notification",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (notification.type) {
                        "friend_accepted" -> "${notification.fromUserName}님이 친구 요청을 수락했습니다"
                        "friend_rejected" -> "${notification.fromUserName}님이 친구 요청을 거절했습니다"
                        else -> "새로운 알림"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = notification.formattedTime,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// 친구 요청 수락/거절 다이얼로그
@Composable
fun FriendRequestDialog(
    notification: Notification,
    onAccept: (Notification) -> Unit,
    onReject: (Notification) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("친구 요청") },
        text = { 
            Text("${notification.fromUserName}님의 친구 요청을 수락하시겠습니까?")
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        onAccept(notification)
                        onDismiss()
                    }
                ) {
                    Text("수락")
                }
                TextButton(
                    onClick = {
                        onReject(notification)
                        onDismiss()
                    }
                ) {
                    Text("거절")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// 메인 NotificationScreen 컴포저블
@Composable
fun NotificationScreen(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser ?: return
    val currentUserId = currentUser.uid
    val context = LocalContext.current
    
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var showDialog by remember { mutableStateOf<Notification?>(null) }
    var loading by remember { mutableStateOf(true) }
    
    // Realtime Database 리스너 설정
    DisposableEffect(currentUserId) {
        val realtimeDb = FirebaseDatabase.getInstance().reference
        val notificationsRef = realtimeDb.child("notifications").child(currentUserId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newNotifications = mutableListOf<Notification>()
                
                for (childSnapshot in snapshot.children) {
                    val fromUserId = childSnapshot.child("fromUserId").getValue(String::class.java) ?: continue
                    val fromUserName = childSnapshot.child("fromUserName").getValue(String::class.java) ?: continue
                    val timestamp = childSnapshot.child("timestamp").getValue(Long::class.java) ?: continue
                    
                    newNotifications.add(
                        Notification(
                            id = childSnapshot.key ?: "",
                            type = "friend_request",
                            fromUserId = fromUserId,
                            fromUserName = fromUserName,
                            timestamp = timestamp
                        )
                    )
                }
                
                // Firestore에서 기존 알림들도 가져오기 (비동기 처리)
                firestore.collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                    .get()
                    .addOnSuccessListener { firestoreSnapshot ->
                        val firestoreNotifications = firestoreSnapshot.documents.mapNotNull { doc ->
                            val type = doc.getString("type") ?: return@mapNotNull null
                            val fromUserId = doc.getString("fromUserId") ?: return@mapNotNull null
                            val fromUserName = doc.getString("fromUserName") ?: return@mapNotNull null
                            val timestamp = doc.getLong("timestamp") ?: return@mapNotNull null
                            
                            Notification(
                                id = doc.id,
                                type = type,
                                fromUserId = fromUserId,
                                fromUserName = fromUserName,
                                timestamp = timestamp
                            )
                        }
                        
                        // 모든 알림을 합치고 시간순으로 정렬
                        notifications = (newNotifications + firestoreNotifications)
                            .sortedByDescending { it.timestamp }
                        loading = false
                    }
                    .addOnFailureListener { e ->
                        notifications = newNotifications.sortedByDescending { it.timestamp }
                        loading = false
                    }
            }
            
            override fun onCancelled(error: DatabaseError) {
                loading = false
            }
        }
        
        notificationsRef.addValueEventListener(listener)
        
        // Cleanup
        onDispose {
            notificationsRef.removeEventListener(listener)
        }
    }
    
    // 친구 요청 수락 처리
    val handleAccept = { notification: Notification ->
        try {
            // Firestore에 친구 관계 추가
            val friendData = mapOf(
                "name" to notification.fromUserName,
                "addedAt" to System.currentTimeMillis()
            )
            
            // 현재 사용자의 친구 목록에 추가
            firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(notification.fromUserId)
                .set(friendData)
            
            // 요청한 사용자의 친구 목록에도 추가
            firestore.collection("users")
                .document(notification.fromUserId)
                .collection("friends")
                .document(currentUserId)
                .set(mapOf(
                    "name" to (auth.currentUser?.displayName ?: auth.currentUser?.email ?: currentUserId),
                    "addedAt" to System.currentTimeMillis()
                ))
            
            // 친구 요청 삭제
            firestore.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .document(notification.fromUserId)
                .delete()
            
            // Realtime Database에서도 삭제
            FirebaseDatabase.getInstance().reference
                .child("notifications")
                .child(currentUserId)
                .child(notification.id)
                .removeValue()
            
            // 수락 알림을 요청한 사용자에게 전송
            val acceptNotification = mapOf(
                "type" to "friend_accepted",
                "fromUserId" to currentUserId,
                "fromUserName" to (auth.currentUser?.displayName ?: auth.currentUser?.email ?: currentUserId),
                "timestamp" to System.currentTimeMillis()
            )
            
            firestore.collection("users")
                .document(notification.fromUserId)
                .collection("notifications")
                .add(acceptNotification)
            
            // Realtime Database에도 수락 알림 저장
            FirebaseDatabase.getInstance().reference
                .child("notifications")
                .child(notification.fromUserId)
                .push()
                .setValue(acceptNotification)
            
            android.widget.Toast.makeText(context, "친구 요청을 수락했습니다", android.widget.Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "오류가 발생했습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // 친구 요청 거절 처리
    val handleReject = { notification: Notification ->
        try {
            // 친구 요청 삭제
            firestore.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .document(notification.fromUserId)
                .delete()
            
            // Realtime Database에서도 삭제
            FirebaseDatabase.getInstance().reference
                .child("notifications")
                .child(currentUserId)
                .child(notification.id)
                .removeValue()
            
            // 거절 알림을 요청한 사용자에게 전송
            val rejectNotification = mapOf(
                "type" to "friend_rejected",
                "fromUserId" to currentUserId,
                "fromUserName" to (auth.currentUser?.displayName ?: auth.currentUser?.email ?: currentUserId),
                "timestamp" to System.currentTimeMillis()
            )
            
            firestore.collection("users")
                .document(notification.fromUserId)
                .collection("notifications")
                .add(rejectNotification)
            
            // Realtime Database에도 거절 알림 저장
            FirebaseDatabase.getInstance().reference
                .child("notifications")
                .child(notification.fromUserId)
                .push()
                .setValue(rejectNotification)
            
            android.widget.Toast.makeText(context, "친구 요청을 거절했습니다", android.widget.Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "오류가 발생했습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 상단 헤더
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = "알림",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        
        // 알림 목록
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.NotificationsNone,
                        contentDescription = "No notifications",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "새로운 알림이 없습니다",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(notifications) { notification ->
                    when (notification.type) {
                        "friend_request" -> {
                            FriendRequestItem(
                                notification = notification,
                                onAccept = handleAccept,
                                onReject = handleReject
                            )
                        }
                        else -> {
                            GeneralNotificationItem(notification = notification)
                        }
                    }
                }
            }
        }
    }
    
    // 다이얼로그 표시
    showDialog?.let { notification ->
        FriendRequestDialog(
            notification = notification,
            onAccept = handleAccept,
            onReject = handleReject,
            onDismiss = { showDialog = null }
        )
    }
}
