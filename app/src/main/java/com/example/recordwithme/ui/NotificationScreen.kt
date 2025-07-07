package com.example.recordwithme.ui

//Android 기본
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

// Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

//날짜 포맷
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Jetpack Compose - 기본
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset

// Jetpack Compose - Material3
import androidx.compose.material3.*

// Jetpack Compose - Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

// Animation
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

//Permissions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

//Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import kotlin.math.roundToInt


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

// 스와이프 삭제 가능한 일반 알림 아이템 컴포저블
@Composable
fun SwipeableGeneralNotificationItem(
    notification: Notification,
    onDelete: (Notification) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 200),
        label = "swipe"
    )

    val density = LocalDensity.current
    val screenWidth = with(density) { 400.dp.toPx() } // 대략적인 카드 너비
    val deleteThreshold = -screenWidth / 2 // 절반까지 슬라이드하면 삭제

    // 알림 카드
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetX < deleteThreshold) {
                            // 삭제 애니메이션과 함께 알림 제거
                            offsetX = -screenWidth
                            onDelete(notification)
                        } else {
                            // 원래 위치로 돌아가기
                            offsetX = 0f
                        }
                    }
                ) { _, dragAmount ->
                    offsetX += dragAmount.x
                    // 오른쪽으로는 스와이프 불가
                    if (offsetX > 0) offsetX = 0f
                    // 왼쪽으로는 최대 화면 너비만큼만
                    if (offsetX < -screenWidth) offsetX = -screenWidth
                }
            },
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
    
    // 알림 권한 요청
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("NotificationScreen", "알림 권한 허용됨")
        } else {
            Log.d("NotificationScreen", "알림 권한 거부됨")
        }
    }

    // 권한 확인 및 요청
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d("NotificationScreen", "알림 권한 이미 허용됨")
                }
                else -> {
                    Log.d("NotificationScreen", "알림 권한 요청")
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    // Realtime Database 리스너 설정
    DisposableEffect(currentUserId) {
        val realtimeDb = FirebaseDatabase.getInstance().reference
        val notificationsRef = realtimeDb.child("notifications").child(currentUserId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("NotificationScreen", "Realtime Database 데이터 변경 감지: ${snapshot.childrenCount}개 항목")
                val newNotifications = mutableListOf<Notification>()
                
                for (childSnapshot in snapshot.children) {
                    Log.d("NotificationScreen", "알림 데이터: ${childSnapshot.key}")
                    val fromUserId = childSnapshot.child("fromUserId").getValue(String::class.java) ?: continue
                    val fromUserName = childSnapshot.child("fromUserName").getValue(String::class.java) ?: continue
                    val timestamp = childSnapshot.child("timestamp").getValue(Long::class.java) ?: continue
                    
                    Log.d("NotificationScreen", "친구 요청 발견: $fromUserName ($fromUserId)")

                    val notification = Notification(
                        id = childSnapshot.key ?: "",
                        type = "friend_request",
                        fromUserId = fromUserId,
                        fromUserName = fromUserName,
                        timestamp = timestamp
                    )

                    newNotifications.add(notification)

                    // 시스템 알림 표시
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            val builder = NotificationCompat.Builder(context, "friend_requests")
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle("새로운 친구 요청")
                                .setContentText("${fromUserName}님이 친구 요청을 보냈습니다")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setAutoCancel(true)

                            with(NotificationManagerCompat.from(context)) {
                                notify(System.currentTimeMillis().toInt(), builder.build())
                            }
                            Log.d("NotificationScreen", "시스템 알림 표시: ${fromUserName}")
                        }
                    }

                    // Realtime Database에서 감지한 친구 요청을 Firestore에도 저장
                    firestore.collection("users")
                        .document(currentUserId)
                        .collection("notifications")
                        .document(childSnapshot.key ?: "")
                        .set(mapOf(
                            "type" to "friend_request",
                            "fromUserId" to fromUserId,
                            "fromUserName" to fromUserName,
                            "timestamp" to timestamp
                        ))
                        .addOnSuccessListener {
                            Log.d("NotificationScreen", "Firestore에 친구 요청 알림 저장 완료")
                        }
                        .addOnFailureListener { e ->
                            Log.e("NotificationScreen", "Firestore 알림 저장 실패: ${e.message}")
                        }
                }
                
                // Firestore에서 기존 알림들도 가져오기 (비동기 처리)
                firestore.collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                    .get()
                    .addOnSuccessListener { firestoreSnapshot ->
                        Log.d("NotificationScreen", "Firestore에서 ${firestoreSnapshot.documents.size}개 알림 가져옴")
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
                        val allNotifications = (newNotifications + firestoreNotifications)
                            .sortedByDescending { it.timestamp }

                        Log.d("NotificationScreen", "총 ${allNotifications.size}개 알림 (Realtime: ${newNotifications.size}, Firestore: ${firestoreNotifications.size})")

                        // 중복 제거 (같은 ID의 알림이 있을 경우)
                        val uniqueNotifications = allNotifications.distinctBy { it.id }
                        notifications = uniqueNotifications
                        loading = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotificationScreen", "Firestore 알림 가져오기 실패: ${e.message}")
                        notifications = newNotifications.sortedByDescending { it.timestamp }
                        loading = false
                    }
            }
            
            override fun onCancelled(error: DatabaseError) {
                loading = false
            }
        }
        
        Log.d("NotificationScreen", "Realtime Database 리스너 설정: $currentUserId")
        notificationsRef.addValueEventListener(listener)
        
        // Cleanup
        onDispose {
            Log.d("NotificationScreen", "Realtime Database 리스너 제거")
            notificationsRef.removeEventListener(listener)
        }
    }
    
    // 친구 요청 수락 처리
    val handleAccept = { notification: Notification ->
        try {
            // 이미 친구인지 확인
            firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(notification.fromUserId)
                .get()
                .addOnSuccessListener { friendDoc ->
                    if (friendDoc.exists()) {
                        android.widget.Toast.makeText(context, "이미 친구입니다", android.widget.Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

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

                    // 현재 사용자의 실제 이름 가져오기
                    firestore.collection("users")
                        .document(currentUserId)
                        .get()
                        .addOnSuccessListener { currentUserDoc ->
                            val currentUserName = if (currentUserDoc.exists()) {
                                val name = currentUserDoc.getString("name")
                                val userId = currentUserDoc.getString("id")
                                when {
                                    !name.isNullOrBlank() -> name
                                    !userId.isNullOrBlank() -> userId
                                    else -> (auth.currentUser?.displayName ?: auth.currentUser?.email ?: currentUserId)
                                }
                            } else {
                                auth.currentUser?.displayName ?: auth.currentUser?.email ?: currentUserId
                            }

                            // 요청한 사용자의 친구 목록에도 추가
                            firestore.collection("users")
                                .document(notification.fromUserId)
                                .collection("friends")
                                .document(currentUserId)
                                .set(mapOf(
                                    "name" to currentUserName,
                                    "addedAt" to System.currentTimeMillis()
                                ))
                                .addOnSuccessListener {
                                    Log.d("NotificationScreen", "상대방 친구 목록에 추가 완료: $currentUserName")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("NotificationScreen", "상대방 친구 목록 추가 실패: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("NotificationScreen", "현재 사용자 정보 가져오기 실패: ${e.message}")
                            // 실패 시 기본값으로 추가
                            firestore.collection("users")
                                .document(notification.fromUserId)
                                .collection("friends")
                                .document(currentUserId)
                                .set(mapOf(
                                    "name" to (auth.currentUser?.displayName ?: auth.currentUser?.email ?: currentUserId),
                                    "addedAt" to System.currentTimeMillis()
                                ))
                        }

                    // 친구 요청 삭제 (양쪽에서)
                    firestore.collection("users")
                        .document(currentUserId)
                        .collection("friendRequests")
                        .document(notification.fromUserId)
                        .delete()

                    firestore.collection("users")
                        .document(notification.fromUserId)
                        .collection("friendRequests")
                        .document(currentUserId)
                        .delete()

                    // Realtime Database에서도 삭제
                    FirebaseDatabase.getInstance().reference
                        .child("notifications")
                        .child(currentUserId)
                        .child(notification.id)
                        .removeValue()
                        .addOnSuccessListener {
                            Log.d("NotificationScreen", "Realtime Database에서 알림 삭제 완료")
                        }
                        .addOnFailureListener { e ->
                            Log.e("NotificationScreen", "Realtime Database 알림 삭제 실패: ${e.message}")
                        }

                    // 친구 요청 수락 알림을 상대방에게만 보냄
                    if (notification.fromUserId != currentUserId) {
                        val acceptNotification = mapOf(
                            "type" to "friend_accepted",
                            "fromUserId" to currentUserId,
                            "fromUserName" to (auth.currentUser?.displayName ?: auth.currentUser?.email ?: currentUserId),
                            "timestamp" to System.currentTimeMillis()
                        )
                        firestore.collection("users")
                            .document(notification.fromUserId) // 친구 요청을 보낸 사람에게만!
                            .collection("notifications")
                            .add(acceptNotification)
                    }

                    // Firestore에서도 알림 삭제
                    firestore.collection("users")
                        .document(currentUserId)
                        .collection("notifications")
                        .document(notification.id)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("NotificationScreen", "Firestore에서 알림 삭제 완료")
                        }
                        .addOnFailureListener { e ->
                            Log.e("NotificationScreen", "Firestore 알림 삭제 실패: ${e.message}")
                        }

                    // UI에서 알림 제거
                    notifications = notifications.filter { it.id != notification.id }
                    android.widget.Toast.makeText(context, "친구 요청을 수락했습니다", android.widget.Toast.LENGTH_SHORT).show()

                }
                .addOnFailureListener { e ->
                    android.widget.Toast.makeText(context, "오류가 발생했습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "오류가 발생했습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // 친구 요청 거절 처리
    val handleReject = { notification: Notification ->
        try {
            // 친구 요청 삭제 (양쪽에서)
            firestore.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .document(notification.fromUserId)
                .delete()

            firestore.collection("users")
                .document(notification.fromUserId)
                .collection("friendRequests")
                .document(currentUserId)
                .delete()

            // Realtime Database에서도 삭제
            FirebaseDatabase.getInstance().reference
                .child("notifications")
                .child(currentUserId)
                .child(notification.id)
                .removeValue()
                .addOnSuccessListener {
                    Log.d("NotificationScreen", "Realtime Database에서 알림 삭제 완료")
                }
                .addOnFailureListener { e ->
                    Log.e("NotificationScreen", "Realtime Database 알림 삭제 실패: ${e.message}")
                }

            // 거절 알림을 요청한 사용자에게 전송 (Firestore만)
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
            
            // Firestore에서도 알림 삭제
            firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(notification.id)
                .delete()
                .addOnSuccessListener {
                    Log.d("NotificationScreen", "Firestore에서 알림 삭제 완료")
                }
                .addOnFailureListener { e ->
                    Log.e("NotificationScreen", "Firestore 알림 삭제 실패: ${e.message}")
                }
            
            // UI에서 알림 제거
            notifications = notifications.filter { it.id != notification.id }
            android.widget.Toast.makeText(context, "친구 요청을 거절했습니다", android.widget.Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "오류가 발생했습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 일반 알림 삭제 처리
    val handleDeleteNotification = { notification: Notification ->
        try {
            // Firestore에서 알림 삭제
            firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(notification.id)
                .delete()
                .addOnSuccessListener {
                    Log.d("NotificationScreen", "Firestore에서 일반 알림 삭제 완료")
                    // UI에서 알림 제거
                    notifications = notifications.filter { it.id != notification.id }
                    android.widget.Toast.makeText(context, "알림을 삭제했습니다", android.widget.Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("NotificationScreen", "Firestore 일반 알림 삭제 실패: ${e.message}")
                    android.widget.Toast.makeText(context, "삭제에 실패했습니다", android.widget.Toast.LENGTH_SHORT).show()
                }
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
                items(
                    items = notifications,
                    key = { it.id } // 각 알림의 고유 ID를 키로 사용
                ) { notification ->
                    when (notification.type) {
                        "friend_request" -> {
                            FriendRequestItem(
                                notification = notification,
                                onAccept = { handleAccept(it) },
                                onReject = { handleReject(it) }
                            )
                        }
                        else -> {
                            SwipeableGeneralNotificationItem(
                                notification = notification,
                                onDelete = { handleDeleteNotification(it) }
                            )
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
            onAccept = { handleAccept(it) },
            onReject = { handleReject(it) },
            onDismiss = { showDialog = null }
        )
    }
}
