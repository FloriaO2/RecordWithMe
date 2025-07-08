package com.example.recordwithme.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 데이터 클래스들
data class Friend(
    val id: String,
    val name: String,
    val mutual: String,
    val profileImageUrl: String? = null,
    val profileImageIsBase64: Boolean = false
) {
    val initial: String get() = name.firstOrNull()?.toString() ?: ""
}

data class User(val id: String, val name: String, val loginType: String, val displayId: String = "", val email: String = "")

// 친구 아이템 컴포저블
@Composable
fun FriendItem(
    friend: Friend,
    isSelected: Boolean = false,
    groupMode: Boolean = false,
    onRemoveClick: (Friend) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("삭제 확인") },
            text = { Text("정말로 ${friend.name}님을 친구 목록에서 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onRemoveClick(friend)
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (screenWidth.value * 0.03f).dp)
    ) {
        Box(
            modifier = Modifier
                .size((screenWidth.value * 0.125f).dp)
                .clip(CircleShape)
                .background(Color(0xFFBDBDBD)),
            contentAlignment = Alignment.Center
        ) {
            if (friend.profileImageUrl != null && friend.profileImageIsBase64) {
                ProfileBase64Image(
                    base64String = friend.profileImageUrl,
                    modifier = Modifier.size((screenWidth.value * 0.125f).dp).clip(CircleShape)
                )
            } else {
                Text(
                    text = friend.initial,
                    color = Color.White,
                    fontSize = (screenWidth.value * 0.05f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width((screenWidth.value * 0.04f).dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(friend.name, fontWeight = FontWeight.Medium, fontSize = (screenWidth.value * 0.04f).sp)
            Text(friend.mutual, color = Color.Gray)
        }

        if (!groupMode) {
            IconButton(onClick = { showConfirmDialog = true }) {
                Icon(Icons.Filled.Close, contentDescription = "Remove")
            }
        }
    }
}

// 친구 검색 다이얼로그 (친구 추가용)
@Composable
fun FriendSearchDialog(
    currentUserId: String,
    currentUserEmail: String?,
    firestore: FirebaseFirestore,
    onDismiss: () -> Unit,
    onFriendAdded: (Friend) -> Unit,
    currentFriendIds: List<String>
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(searchText) {
        if (searchText.isBlank()) {
            searchResults = emptyList()
        } else {
            loading = true
            try {
                val querySnapshot = firestore.collection("users").get().await()
                searchResults = querySnapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val userId = doc.getString("id") ?: ""
                    val name = doc.getString("name") ?: ""
                    val email = doc.getString("email") ?: ""
                    val loginType = if (userId.isNotEmpty()) "normal" else "google"
                    val displayName = name.ifBlank { userId.ifBlank { id } }
                    val displayId = userId

                    // 이메일이 @recordwith.me로 끝나지 않는 경우에만 이메일 검색에 포함
                    val emailSearchable = !email.endsWith("@recordwith.me") && email.isNotEmpty()

                    if ((displayName.contains(searchText, ignoreCase = true) ||
                                displayId.contains(searchText, ignoreCase = true) ||
                                (emailSearchable && email.contains(searchText, ignoreCase = true))) &&
                        id != currentUserId &&
                        !currentFriendIds.contains(id)
                    ) {
                        User(id, displayName, loginType, displayId, email)
                    } else null
                }
            } catch (e: Exception) {
                searchResults = emptyList()
            }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("친구 검색") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("이름, 아이디 또는 이메일로 검색") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height((screenHeight.value * 0.015f).dp))

                if (loading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (searchResults.isEmpty()) {
                        if (searchText.isNotBlank()) {
                            Text("검색 결과 없음", color = Color.Gray)
                        } else {
                            Text("검색어를 입력하세요", color = Color.Gray)
                        }
                    } else {
                        Column {
                            searchResults.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = (screenHeight.value * 0.01f).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        // @recordwith.me로 끝나지 않는 사용자는 이메일을 표시
                                        if (!user.email.endsWith("@recordwith.me") && user.email.isNotEmpty()) {
                                            Text(user.email, fontWeight = FontWeight.Medium)
                                        } else {
                                            Text(user.name, fontWeight = FontWeight.Medium)
                                        }
                                        // @recordwith.me 사용자는 기존처럼 아이디 표시
                                        if (user.email.endsWith("@recordwith.me") && user.displayId.isNotEmpty()) {
                                            Text("@${user.displayId}", fontSize = (screenWidth.value * 0.03f).sp, color = Color.Gray)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            // 코루틴 스코프에서 비동기 작업 실행
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                try {
                                                    // 현재 사용자 정보를 Firestore에서 가져오기
                                                    val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
                                                    val currentUserName = currentUserDoc.getString("name") ?: ""
                                                    val currentUserEmailFromFirestore = currentUserDoc.getString("email") ?: ""
                                                    
                                                    // fromUserName 결정: @recordwith.me면 name(있을 때만), 아니면 이메일 전체
                                                    val isRecordWithMe = currentUserEmailFromFirestore.endsWith("@recordwith.me")
                                                    val fromUserName = when {
                                                        isRecordWithMe && currentUserName.isNotEmpty() -> currentUserName
                                                        currentUserEmailFromFirestore.isNotEmpty() -> currentUserEmailFromFirestore
                                                        else -> currentUserId
                                                    }
                                                    
                                                    val requestData = mapOf(
                                                        "fromUserId" to currentUserId,
                                                        "fromUserName" to fromUserName,
                                                        "timestamp" to System.currentTimeMillis()
                                                    )

                                                    // Firestore에 친구 신청 저장
                                                    firestore.collection("users")
                                                        .document(user.id)
                                                        .collection("friendRequests")
                                                        .document(currentUserId)
                                                        .set(requestData)
                                                        .addOnSuccessListener {
                                                            // Firestore notifications에도 저장
                                                            val notificationData = mapOf(
                                                                "type" to "friendRequest",
                                                                "fromUserId" to currentUserId,
                                                                "fromUserName" to fromUserName,
                                                                "timestamp" to System.currentTimeMillis()
                                                            )
                                                            
                                                            firestore.collection("users")
                                                                .document(user.id)
                                                                .collection("notifications")
                                                                .add(notificationData)
                                                                .addOnSuccessListener { doc ->
                                                                    // ✅ Realtime Database에도 저장 (알림용)
                                                                    val realtimeDb = FirebaseDatabase.getInstance().reference
                                                                    val realtimeNotificationData = notificationData.toMutableMap()
                                                                    realtimeNotificationData["id"] = doc.id
                                                                    realtimeDb.child("notifications")
                                                                        .child(user.id)  // 수신자 ID 경로
                                                                        .child(doc.id)
                                                                        .setValue(realtimeNotificationData)
                                                                }

                                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "친구 요청을 보냈습니다",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                                onDismiss()
                                                            }
                                                        }
                                                        .addOnFailureListener { e ->
                                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "친구 추가에 실패했습니다: ${e.message}",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                } catch (e: Exception) {
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "친구 요청 중 오류가 발생했습니다: ${e.message}",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape((screenWidth.value * 0.03f).dp),
                                        contentPadding = PaddingValues(
                                            horizontal = (screenWidth.value * 0.03f).dp, 
                                            vertical = (screenHeight.value * 0.005f).dp
                                        )
                                    ) {
                                        Text("친구 신청", fontSize = (screenWidth.value * 0.0325f).sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

// 전역 상태로 groupMode 관리
object GroupModeState {
    var isGroupMode by mutableStateOf(false)
}

// Helper function for display email
fun getDisplayEmail(email: String?): String {
    return if (email != null && email.endsWith("@recordwith.me")) {
        email.removeSuffix("@recordwith.me")
    } else {
        email ?: ""
    }
}

// 프로필 Base64 이미지 컴포저블
@Composable
fun ProfileBase64Image(base64String: String, modifier: Modifier = Modifier) {
    var bitmap by remember(base64String) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(base64String) {
        try {
            val bytes = Base64.decode(base64String, Base64.DEFAULT)
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            // Base64 디코딩 실패 시 처리
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "프로필 이미지",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.background(Color.LightGray), contentAlignment = Alignment.Center) {
            Text("이미지 로딩 중...", color = Color.White)
        }
    }
}

// 최종 ProfileScreen 컴포저블
@Composable
fun ProfileScreen(
    navController: NavController,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser ?: return
    val currentUserId = currentUser.uid
    val currentUserEmail = currentUser.email
    val context = LocalContext.current

    var showFriendDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showNoFriendsDialog by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    val selectedFriendIds = remember { mutableStateListOf<String>() }
    var userName by remember { mutableStateOf("") }
    var userDisplayId by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var groupNote by remember { mutableStateOf("") }
    var friendsLoaded by remember { mutableStateOf(false) }
    
    // 프로필 이미지 관련 상태
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var profileImageIsBase64 by remember { mutableStateOf(false) }
    
    // View Details 상태 관리
    var showDetails by remember { mutableStateOf(false) }

    // 전역 상태 사용
    var groupMode by remember { mutableStateOf(GroupModeState.isGroupMode) }
    
    // 전역 상태 변화 감지
    LaunchedEffect(GroupModeState.isGroupMode) {
        groupMode = GroupModeState.isGroupMode
    }

    // groupMode가 true가 되었을 때 친구가 없으면 팝업 표시
    LaunchedEffect(groupMode, friendsLoaded) {
        println("ProfileScreen: groupMode changed to = $groupMode, friends count: ${friends.size}, friendsLoaded: $friendsLoaded")
        // 친구 목록이 로드된 후에만 체크
        if (groupMode && friendsLoaded) {
            if (friends.isNotEmpty()) {
                // 친구가 있으면 그룹 모드 활성화
                println("ProfileScreen: Friends available, enabling group mode")
            } else {
                // 친구가 없을 때만 안내 팝업 표시
                println("ProfileScreen: No friends available, showing dialog")
                showNoFriendsDialog = true
                GroupModeState.isGroupMode = false
            }
        }
    }

    val listState = rememberLazyListState()
    val profileHeightDp = 320.dp
    val density = LocalDensity.current
    val maxOffset = with(density) { profileHeightDp.toPx() }

    val profileAlpha by remember {
        derivedStateOf {
            val offsetPx =
                listState.firstVisibleItemScrollOffset.toFloat() +
                        listState.firstVisibleItemIndex * 1000f
            ((maxOffset - offsetPx) / maxOffset).coerceIn(0f, 1f)
        }
    }

    // 화면 크기에 따른 반응형 설정
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // 화면 크기에 따른 동적 값 계산
    val horizontalPadding = (screenWidth.value * 0.06f).dp
    
    val detailsWidth = (screenWidth.value * 0.45f).dp
    
    // 프로필 이미지 크기를 화면 너비에 따라 지속적으로 변경
    val profileImageSize = (screenWidth.value * 0.35f).dp
    
    val profileImageOffset by animateFloatAsState(
        targetValue = if (showDetails) -(screenWidth.value * 0.03f) / 50f else 0f,
        animationSpec = tween(1000),
        label = "profileImageOffset"
    )

    // 프로필 이미지 세로 위치 조정 (위로 이동) - 화면 세로 크기에 비례
    val profileImageVerticalOffset = -(screenHeight.value * 0.06f)

    // 프로필 텍스트 이동 애니메이션 (구글 로그일 때는 고정, 일반 로그일 때만 이동)
    val isGoogleLogin = !(currentUserEmail?.endsWith("@recordwith.me") == true)
    val profileTextOffset by animateFloatAsState(
        targetValue = if (showDetails && !isGoogleLogin) -(screenWidth.value * 0.03f) / 50f else 0f,
        animationSpec = tween(1000),
        label = "profileTextOffset"
    )

    // Details 우측에서 등장 애니메이션 (화면 크기에 따른 상대적 위치)
    val detailsOffset by animateFloatAsState(
        targetValue = if (showDetails) 0f else (3f+screenWidth.value / 300f),
        animationSpec = tween(1000),
        label = "detailsOffset"
    )

    // 사용자 정보 불러오기
    LaunchedEffect(currentUserId) {
        try {
            // 사용자 정보 불러오기
            val userDoc = firestore.collection("users")
                .document(currentUserId)
                .get()
                .await()

            if (userDoc.exists()) {
                userName = userDoc.getString("name") ?: ""
                userDisplayId = userDoc.getString("id") ?: ""
                profileImageUrl = userDoc.getString("profileImageUrl")
                profileImageIsBase64 = userDoc.getBoolean("profileImageIsBase64") ?: false
            }
        } catch (_: Exception) {}
    }
    
    // 친구 목록 리스너 정리
    DisposableEffect(currentUserId) {
        val friendsRef = firestore.collection("users")
            .document(currentUserId)
            .collection("friends")
        
        val friendListeners = mutableMapOf<String, ListenerRegistration>()
        val friendsList = mutableListOf<Friend>()
        
        val mainListener = friendsRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                friendsList.clear()
                // 기존 리스너 해제
                friendListeners.values.forEach { it.remove() }
                friendListeners.clear()
                
                snapshot.documents.forEach { doc ->
                    val id = doc.id
                    // 각 친구의 사용자 문서에 실시간 리스너 등록
                    val listener = firestore.collection("users").document(id)
                        .addSnapshotListener { friendUserDoc, _ ->
                            if (friendUserDoc != null && friendUserDoc.exists()) {
                                val name = friendUserDoc.getString("name") ?: ""
                                val email = friendUserDoc.getString("email") ?: ""
                                val profileImageUrl = friendUserDoc.getString("profileImageUrl")
                                val profileImageIsBase64 = friendUserDoc.getBoolean("profileImageIsBase64") ?: false
                                val displayName = if (name.isNotBlank()) name else email
                                val friend = Friend(
                                    id = id,
                                    name = displayName,
                                    mutual = "친구",
                                    profileImageUrl = profileImageUrl,
                                    profileImageIsBase64 = profileImageIsBase64
                                )
                                // 중복 방지
                                friendsList.removeAll { it.id == id }
                                friendsList.add(friend)
                                // 모든 친구 정보가 모이면 friends 상태 업데이트
                                if (friendsList.size == snapshot.documents.size) {
                                    friends = friendsList.toList()
                                    friendsLoaded = true
                                }
                            }
                        }
                    friendListeners[id] = listener
                }
                // 친구가 없는 경우
                if (snapshot.documents.isEmpty()) {
                    friends = emptyList()
                    friendsLoaded = true
                }
            }
        }
        
        onDispose {
            mainListener.remove()
            friendListeners.values.forEach { it.remove() }
        }
    }

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    
    // 갤러리 런처
    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            // 프로필 이미지를 Base64로 변환하여 Firestore에 저장
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                    
                    // Firestore에 프로필 이미지 저장
                    firestore.collection("users")
                        .document(currentUserId)
                        .update(
                            mapOf(
                                "profileImageUrl" to base64String,
                                "profileImageIsBase64" to true
                            )
                        )
                        .addOnSuccessListener {
                            profileImageUrl = base64String
                            profileImageIsBase64 = true
                            android.widget.Toast.makeText(context, "프로필 이미지가 변경되었습니다", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            android.widget.Toast.makeText(context, "프로필 이미지 변경 실패: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "이미지 처리 실패: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 전체 화면 Box로 감싸서 버튼을 고정 위치에 띄우기
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {

            // 로딩 상태에 따른 화면 표시
            if (friendsLoaded) {
                // 로딩 완료: 모든 프로필 요소 표시
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(profileAlpha)
                        .padding(vertical = (screenHeight.value * 0.012f).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile 텍스트는 항상 상단 중앙에 고정
                    Text(
                        "Profile", 
                        fontSize = (screenWidth.value * 0.06f).sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            top = (screenHeight.value * 0.012f).dp, 
                            bottom = (screenHeight.value * 0.012f).dp
                        )
                    )

                    // 프로필과 상세 정보를 포함하는 Box
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // 좌측: 프로필 이미지 (중앙에서 좌측으로 이동)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(start = (screenWidth.value * 0.00f).dp)
                                .offset(
                                    x = with(LocalDensity.current) { 
                                        (profileImageOffset * 1200).toDp()
                                    },
                                    y = with(LocalDensity.current) { 
                                        profileImageVerticalOffset.toDp()
                                    }
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(profileImageSize)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0E0E0))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                profileImageLauncher.launch("image/*")
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (profileImageUrl != null) {
                                    // 프로필 이미지가 있으면 이미지 표시
                                    if (profileImageIsBase64) {
                                        ProfileBase64Image(
                                            base64String = profileImageUrl!!,
                                            modifier = Modifier
                                                .size(profileImageSize)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        // URL 이미지인 경우 (현재는 Base64만 사용)
                                        Text(
                                            text = "이미지 오류",
                                            fontSize = (profileImageSize.value * 0.2).sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    // 프로필 이미지가 없으면 이니셜 표시
                                    val displayText = when {
                                        userName.isNotEmpty() -> userName.first().uppercaseChar().toString()
                                        userDisplayId.isNotEmpty() -> userDisplayId.first().uppercaseChar().toString()
                                        currentUserEmail?.isNotEmpty() == true -> currentUserEmail.first().uppercaseChar().toString()
                                        else -> "U"
                                    }

                                    Text(
                                        text = displayText,
                                        fontSize = (profileImageSize.value * 0.35).sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 프로필 텍스트 (이미지와 별도로 이동)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset(
                                x = with(LocalDensity.current) { 
                                    (profileTextOffset * 1200).toDp()
                                }
                            )
                        ) {
                            Spacer(modifier = Modifier.height(profileImageSize + (screenHeight.value * 0.025f).dp)) // 이미지 높이 + 간격

                            val displayEmail = getDisplayEmail(currentUserEmail)
                            Text(displayEmail, fontSize = (screenWidth.value * 0.055f).sp, fontWeight = FontWeight.Bold)
                        }

                        // 우측: 상세 정보 (화면 바깥에서 들어옴)
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(
                                    start = (screenWidth.value * 0.10f).dp,
                                    end = (screenWidth.value * 0.06f).dp
                                )
                                .width(detailsWidth)
                                .offset(
                                    x = with(LocalDensity.current) { 
                                        (detailsOffset * (screenWidth.value * 0.95f)).toDp()
                                    },
                                    y = if (isGoogleLogin) -(screenHeight.value * 0.013f).dp else (screenHeight.value * 0.025f).dp
                                )
                        ) {
                            // 상세 정보 내용
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = (screenHeight.value * 0.02f).dp, 
                                        bottom = (screenHeight.value * 0.02f).dp
                                    )
                            ) {
                                // 구글 로그인인지 확인 (@recordwith.me가 아닌 경우 모두 구글 로그인)
                                val isGoogleLogin = !(currentUserEmail?.endsWith("@recordwith.me") == true)
                                
                                if (isGoogleLogin) {
                                    // 구글 로그인: 이메일/친구수만 표시
                                    val email = currentUserEmail ?: "설정되지 않음"
                                    if (!email.endsWith("@recordwith.me")) {
                                        val atIdx = email.indexOf("@")
                                        val displayEmail = if (atIdx > 0) {
                                            email.substring(0, atIdx) + "\n" + email.substring(atIdx)
                                        } else {
                                            email
                                        }
                                        DetailItem("이메일", displayEmail, valueTextAlign = TextAlign.End)
                                    }
                                    DetailItem("친구 수", "${friends.size}명")
                                    // 회원 탈퇴 텍스트 버튼 (RecordWithMe 계정용)
                                    TextButton(
                                        onClick = { showWithdrawDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset(
                                                y = -(screenHeight.value * 0.015f).dp,
                                                x = (screenWidth.value * 0.03f).dp
                                            )
                                    ) {
                                        Text(
                                            "회원 탈퇴",
                                            color = Color(0xFF8B0000),
                                            fontSize = (screenWidth.value * 0.03f).sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                } else {
                                    // 일반 로그인: 조건 분기
                                    if (currentUserEmail?.endsWith("@recordwith.me") == true) {
                                        DetailItem("이름", userName.ifEmpty { "설정되지 않음" })
                                        DetailItem("아이디", userDisplayId.ifEmpty { "설정되지 않음" })
                                    } else {
                                        if (!currentUserEmail.isNullOrBlank()) {
                                            val email = currentUserEmail
                                            val atIdx = email.indexOf("@")
                                            val displayEmail = if (atIdx > 0) {
                                                email.substring(0, atIdx) + "\n" + email.substring(atIdx)
                                            } else {
                                                email
                                            }
                                            DetailItem("이메일", displayEmail, valueTextAlign = TextAlign.End)
                                        }
                                    }
                                    DetailItem("친구 수", "${friends.size}명")
                                    TextButton(
                                        onClick = { showWithdrawDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset(
                                                y = -(screenHeight.value * 0.015f).dp,
                                                x = (screenWidth.value * 0.03f).dp
                                            )
                                    ) {
                                        Text(
                                            "회원 탈퇴",
                                            color = Color(0xFF8B0000),
                                            fontSize = (screenWidth.value * 0.03f).sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height((screenHeight.value * 0.02f).dp))

                    // 버튼 너비를 화면 크기에 따라 동적으로 조정
                    val buttonWidth = (screenWidth.value * 0.85f).dp
                    Button(
                        onClick = { showDetails = !showDetails },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0)),
                        shape = RoundedCornerShape((screenWidth.value * 0.06f).dp),
                        modifier = Modifier.width(buttonWidth)
                    ) {
                        Text(
                            if (showDetails) "Close Details" else "View Details", 
                            color = Color.Black
                        )
                    }
                }
            } else {
                // 로딩 중: Profile 텍스트와 로딩 애니메이션만 표시
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = (screenHeight.value * 0.012f).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Profile", 
                        fontSize = (screenWidth.value * 0.06f).sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            top = (screenHeight.value * 0.012f).dp, 
                            bottom = (screenHeight.value * 0.012f).dp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height((screenHeight.value * 0.125f).dp))
                    
                    CircularProgressIndicator(
                        modifier = Modifier.size((screenHeight.value * 0.06f).dp),
                        color = Color(0xFFE0E0E0)
                    )
                }
            }

            // Friends 영역 (로딩 완료 후에만 표시)
            if (friendsLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .height((screenHeight.value * 0.06f).dp)
                        .padding(horizontal = horizontalPadding * 1.7f, vertical = (screenHeight.value * 0.008f).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Friends", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy((screenWidth.value * 0.02f).dp)) {
                            val smallButtonWidth = (screenWidth.value * 0.17f).dp
                            Button(
                                onClick = { showFriendDialog = true },
                                modifier = Modifier.width(smallButtonWidth).height((screenHeight.value * 0.06f).dp),
                                shape = RoundedCornerShape(10),
                                border = BorderStroke((screenWidth.value * 0.005f).dp, if (showFriendDialog) Color(0xFF424242) else Color.Gray),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (showFriendDialog) Color(0x59282828) else Color.Transparent
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = (screenWidth.value * 0.015f).dp, 
                                    vertical = (screenHeight.value * 0.005f).dp
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("+", fontSize = (screenWidth.value * 0.04f).sp, fontWeight = FontWeight.Bold, color = if (showFriendDialog) Color(0xFF424242) else Color.Black)
                                    Spacer(modifier = Modifier.width((screenWidth.value * 0.01f).dp))
                                    Icon(
                                        Icons.Filled.Person, 
                                        contentDescription = "Add Friend", 
                                        modifier = Modifier.size((screenWidth.value * 0.0625f).dp), 
                                        tint = if (showFriendDialog) Color(0xFF424242) else Color.Black
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    println("ProfileScreen: Group button clicked, friends count: ${friends.size}")
                                    if (friends.isEmpty()) {
                                        // 친구가 없으면 안내 팝업을 띄우고 그룹 모드 해제
                                        println("ProfileScreen: No friends, showing dialog")
                                        showNoFriendsDialog = true
                                        GroupModeState.isGroupMode = false
                                    } else {
                                        // 친구가 있으면 그룹 모드 토글
                                        println("ProfileScreen: Friends available, toggling group mode")
                                        GroupModeState.isGroupMode = !GroupModeState.isGroupMode
                                        if (!GroupModeState.isGroupMode) {
                                            selectedFriendIds.clear()
                                        }
                                    }
                                },
                                modifier = Modifier.width(smallButtonWidth).height((screenHeight.value * 0.06f).dp),
                                shape = RoundedCornerShape(10),
                                border = BorderStroke((screenWidth.value * 0.005f).dp, if (groupMode) Color(0xBF424242) else Color.Gray),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (groupMode) Color(0x59282828) else Color.Transparent
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = (screenWidth.value * 0.015f).dp, 
                                    vertical = (screenHeight.value * 0.005f).dp
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("+", fontSize = (screenWidth.value * 0.04f).sp, fontWeight = FontWeight.Bold, color = if (groupMode) Color(0xFF424242) else Color.Black)
                                    Spacer(modifier = Modifier.width((screenWidth.value * 0.01f).dp))
                                    Icon(
                                        Icons.Filled.Group, 
                                        contentDescription = "Add Group", 
                                        modifier = Modifier.size((screenWidth.value * 0.07f).dp), 
                                        tint = if (groupMode) Color(0xFF424242) else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (friendsLoaded && friends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = (screenHeight.value * 0.06f).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (groupMode) {
                            "그룹을 만들기 위해서는 먼저 친구를 추가해주세요.\n친구 추가 버튼을 눌러 친구를 추가해보세요."
                        } else {
                            "친구가 없습니다.\n친구 추가 버튼을 눌러 친구를 추가해보세요."
                        },
                        color = Color.Gray,
                        fontSize = (screenWidth.value * 0.035f).sp,
                        lineHeight = (screenHeight.value * 0.0375f).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (friendsLoaded) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = horizontalPadding * 0.7f, vertical = (screenHeight.value * 0.005f).dp),
                    verticalArrangement = Arrangement.spacedBy((screenHeight.value * 0.015f).dp)
                ) {
                    items(friends.sortedBy { it.name }) { friend ->
                        val isSelected = friend.id in selectedFriendIds

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (groupMode) {
                                println("ProfileScreen: Rendering checkbox for friend ${friend.name}, groupMode = $groupMode")
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (it) selectedFriendIds.add(friend.id)
                                        else selectedFriendIds.remove(friend.id)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color.White,
                                        checkmarkColor = Color.Black,
                                        uncheckedColor = Color.Gray
                                    )
                                )
                            } else {
                                println("ProfileScreen: Not rendering checkbox, groupMode = $groupMode")
                            }

                            FriendItem(
                                friend = friend,
                                isSelected = isSelected,
                                groupMode = groupMode,
                                onRemoveClick = { toRemove ->
                                    firestore.collection("users").document(currentUserId)
                                        .collection("friends").document(toRemove.id).delete()
                                    // 쌍방 삭제: 상대방 friends에서도 나를 삭제
                                    firestore.collection("users").document(toRemove.id)
                                        .collection("friends").document(currentUserId).delete()
                                    friends = friends.filter { it.id != toRemove.id }
                                    selectedFriendIds.remove(toRemove.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ✅ 하단 중앙 고정 버튼 (오버레이 방식, 배경 없음)
        if (friendsLoaded && groupMode && selectedFriendIds.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = { showGroupDialog = true },
                    modifier = Modifier
                                            .padding(bottom = (screenHeight.value * 0.03f).dp)
                    .wrapContentWidth()
                    .height((screenHeight.value * 0.06f).dp),
                shape = RoundedCornerShape((screenWidth.value * 0.05f).dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF90CAF9),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(
                    horizontal = (screenWidth.value * 0.06f).dp, 
                    vertical = (screenHeight.value * 0.0125f).dp
                )
            ) {
                Text("그룹 추가하기", fontSize = (screenWidth.value * 0.04f).sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 그룹 생성 다이얼로그
        if (showGroupDialog) {
            AlertDialog(
                onDismissRequest = { showGroupDialog = false },
                title = { Text("그룹 생성") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("그룹 이름") }
                        )
                        Spacer(Modifier.height((screenHeight.value * 0.01f).dp))
                        OutlinedTextField(
                            value = groupNote,
                            onValueChange = { groupNote = it },
                            label = { Text("메모") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // 코루틴 스코프에서 비동기 작업 실행
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                try {
                                    // 그룹 생성 로직
                                    val trimmedGroupName = groupName.trim()
                                    val trimmedGroupNote = groupNote.trim()
                                    val groupCreator = currentUserId
                                    val groupId = trimmedGroupName // 그룹 이름을 ID로 사용

                                    // 1. Firestore groups 컬렉션에 그룹 생성
                                    val groupData = mapOf(
                                        "name" to trimmedGroupName,
                                        "note" to trimmedGroupNote,
                                        "creator" to groupCreator,
                                        "members" to listOf(currentUserId), // 생성자만 초기 멤버
                                        "createdAt" to com.google.firebase.Timestamp.now()
                                    )

                                    firestore.collection("groups").document(groupId).set(groupData).await()

                                    // 2. 생성자를 그룹 멤버로 추가 (users/{uid}/groups)
                                    val memberGroupData = groupData + ("groupId" to groupId)
                                    firestore.collection("users")
                                        .document(currentUserId)
                                        .collection("groups")
                                        .document(groupId)
                                        .set(memberGroupData)
                                    
                                    // 3. 현재 사용자 정보를 Firestore에서 가져오기
                                    if (currentUserId == null) {
                                        println("ProfileScreen: currentUserId is null")
                                        return@launch
                                    }
                                    val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
                                    val currentUserName = currentUserDoc.getString("name") ?: ""
                                    val currentUserEmail = currentUserDoc.getString("email") ?: ""
                                    
                                    // fromUserName: name > email > uid 순서로 표시
                                    val fromUserName = when {
                                        currentUserName.isNotEmpty() -> currentUserName
                                        currentUserEmail.isNotEmpty() -> currentUserEmail
                                        else -> currentUserId
                                    }
                                    
                                    println("ProfileScreen: fromUserName determined: $fromUserName")
                                    
                                    // 4. 선택된 친구들에게 그룹 초대 보내기
                                    selectedFriendIds.forEach { friendId ->
                                        println("ProfileScreen: Sending group invite to $friendId for group $groupId")
                                        
                                        // Firestore groupInvites
                                        val inviteData = mapOf(
                                            "groupId" to groupId,
                                            "groupName" to trimmedGroupName,
                                            "groupNote" to trimmedGroupNote,
                                            "inviterId" to currentUserId,
                                            "inviterName" to fromUserName,
                                            "invitedAt" to com.google.firebase.Timestamp.now(),
                                            "status" to "pending"
                                        )
                                        
                                        firestore.collection("users")
                                            .document(friendId)
                                            .collection("groupInvites")
                                            .document(groupId)
                                            .set(inviteData)
                                        
                                        // 알림 전송 (Firestore)
                                        val inviteNotification = mapOf(
                                            "type" to "groupInvite",
                                            "fromUserId" to currentUserId,
                                            "fromUserName" to fromUserName,
                                            "groupId" to groupId,
                                            "groupName" to trimmedGroupName,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        
                                        firestore.collection("users")
                                            .document(friendId)
                                            .collection("notifications")
                                            .add(inviteNotification)
                                        
                                        // 알림 전송 (Realtime DB)
                                        val ref = FirebaseDatabase.getInstance().reference
                                            .child("notifications")
                                            .child(friendId)
                                            .push()
                                        val notificationId = ref.key ?: ""
                                        val inviteNotificationWithId = inviteNotification.toMutableMap()
                                        inviteNotificationWithId["id"] = notificationId
                                        ref.setValue(inviteNotificationWithId)
                                    }

                                    // UI 업데이트는 메인 스레드에서
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        groupName = ""
                                        groupNote = ""
                                        selectedFriendIds.clear()
                                        GroupModeState.isGroupMode = false
                                        showGroupDialog = false

                                        android.widget.Toast.makeText(
                                            context,
                                            "그룹이 생성되었습니다.\n친구들에게 초대를 보냈습니다.",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()

                                        navController.popBackStack() // 그룹 생성 후 GroupScreen으로 돌아가기
                                    }
                                } catch (e: Exception) {
                                    // 에러 처리
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "그룹 생성 중 오류가 발생했습니다: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        enabled = groupName.isNotBlank() && selectedFriendIds.isNotEmpty()
                    ) {
                        Text("그룹 초대하기")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        groupName = ""
                        groupNote = ""
                        showGroupDialog = false
                    }) {
                        Text("취소")
                    }
                }
            )
        }
    }

    // 친구 추가 검색 다이얼로그
    if (showFriendDialog) {
        FriendSearchDialog(
            currentUserId = currentUserId,
            currentUserEmail = currentUserEmail,
            firestore = firestore,
            onDismiss = { showFriendDialog = false },
            onFriendAdded = { newFriend -> friends = friends + newFriend },
            currentFriendIds = friends.map { it.id }
        )
    }

    // 친구가 없을 때 안내 다이얼로그
    if (showNoFriendsDialog) {
        AlertDialog(
            onDismissRequest = { showNoFriendsDialog = false },
            title = { Text("친구가 필요합니다") },
            text = { Text("그룹을 만들기 위해서는 먼저 친구를 추가해주세요.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNoFriendsDialog = false
                        showFriendDialog = true
                    }
                ) {
                    Text("친구 추가하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoFriendsDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 회원 탈퇴 확인 다이얼로그
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("정말로 탈퇴하시겠습니까?") },
            text = { Text("탈퇴 시 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWithdrawDialog = false
                        isProcessing = true
                        // 탈퇴 처리 함수 호출
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                // 0. Firebase Auth에서 사용자 계정 삭제
                                val currentUser = auth.currentUser
                                if (currentUser != null) {
                                    currentUser.delete().await()
                                }
                                
                                // 1. 모든 groups/*/members에서 해당 uid를 삭제 (메인 문서 삭제 전에)
                                val groupsSnapshot = firestore.collection("groups").get().await()
                                groupsSnapshot.documents.forEach { groupDoc ->
                                    val members = groupDoc.get("members") as? List<String> ?: emptyList()
                                    if (members.contains(currentUserId)) {
                                        val updatedMembers = members.filter { it != currentUserId }
                                        
                                        if (updatedMembers.isEmpty()) {
                                            // 멤버가 없으면 그룹 완전 삭제
                                            firestore.collection("groups")
                                                .document(groupDoc.id)
                                                .delete()
                                                .await()
                                            
                                            // 그룹 캘린더의 사진들 삭제
                                            val photosSnapshot = firestore.collection("groups")
                                                .document(groupDoc.id)
                                                .collection("photos")
                                                .get()
                                                .await()
                                            
                                            photosSnapshot.documents.forEach { photoDoc ->
                                                firestore.collection("groups")
                                                    .document(groupDoc.id)
                                                    .collection("photos")
                                                    .document(photoDoc.id)
                                                    .delete()
                                                    .await()
                                            }
                                            
                                            // 모든 사용자의 개인 그룹 목록에서도 해당 그룹 삭제
                                            val allUsersSnapshot = firestore.collection("users").get().await()
                                            allUsersSnapshot.documents.forEach { userDoc ->
                                                firestore.collection("users")
                                                    .document(userDoc.id)
                                                    .collection("groups")
                                                    .document(groupDoc.id)
                                                    .delete()
                                            }
                                        } else {
                                            // 멤버가 있으면 멤버 목록만 업데이트
                                            firestore.collection("groups")
                                                .document(groupDoc.id)
                                                .update("members", updatedMembers)
                                                .await()
                                        }
                                    }
                                }
                                
                                // 2. 모든 users/*/groups/{groupId}에서 탈퇴한 사용자 삭제 (메인 문서 삭제 전에)
                                val allUsersSnapshot = firestore.collection("users").get().await()
                                allUsersSnapshot.documents.forEach { userDoc ->
                                    val userGroupsSnapshot = firestore.collection("users")
                                        .document(userDoc.id)
                                        .collection("groups")
                                        .get()
                                        .await()
                                    
                                    userGroupsSnapshot.documents.forEach { groupDoc ->
                                        val groupMembers = groupDoc.get("members") as? List<String> ?: emptyList()
                                        if (groupMembers.contains(currentUserId)) {
                                            val updatedGroupMembers = groupMembers.filter { it != currentUserId }
                                            firestore.collection("users")
                                                .document(userDoc.id)
                                                .collection("groups")
                                                .document(groupDoc.id)
                                                .update("members", updatedGroupMembers)
                                                .await()
                                        }
                                    }
                                }
                                
                                // 3. 탈퇴한 사용자의 groups 컬렉션 삭제
                                val currentUserGroupsSnapshot = firestore.collection("users")
                                    .document(currentUserId)
                                    .collection("groups")
                                    .get()
                                    .await()
                                
                                currentUserGroupsSnapshot.documents.forEach { groupDoc ->
                                    firestore.collection("users")
                                        .document(currentUserId)
                                        .collection("groups")
                                        .document(groupDoc.id)
                                        .delete()
                                        .await()
                                }
                                
                                // 4. 탈퇴한 사용자의 친구 목록 삭제
                                val currentUserFriendsSnapshot = firestore.collection("users")
                                    .document(currentUserId)
                                    .collection("friends")
                                    .get()
                                    .await()
                                
                                currentUserFriendsSnapshot.documents.forEach { friendDoc ->
                                    firestore.collection("users")
                                        .document(currentUserId)
                                        .collection("friends")
                                        .document(friendDoc.id)
                                        .delete()
                                        .await()
                                }

                                // 5. 탈퇴한 사용자의 알림 삭제
                                val currentUserNotificationsSnapshot = firestore.collection("users")
                                    .document(currentUserId)
                                    .collection("notifications")
                                    .get()
                                    .await()
                                
                                currentUserNotificationsSnapshot.documents.forEach { notificationDoc ->
                                    firestore.collection("users")
                                        .document(currentUserId)
                                        .collection("notifications")
                                        .document(notificationDoc.id)
                                        .delete()
                                        .await()
                                }
                                
                                // 6. 탈퇴한 사용자의 그룹 초대 삭제
                                val currentUserGroupInvitesSnapshot = firestore.collection("users")
                                    .document(currentUserId)
                                    .collection("groupInvites")
                                    .get()
                                    .await()
                                
                                currentUserGroupInvitesSnapshot.documents.forEach { inviteDoc ->
                                    firestore.collection("users")
                                        .document(currentUserId)
                                        .collection("groupInvites")
                                        .document(inviteDoc.id)
                                        .delete()
                                        .await()
                                }
                                
                                // 7. users/{uid} 메인 문서 삭제 (모든 참조 제거 후)
                                firestore.collection("users").document(currentUserId).delete().await()

                                // 8. 모든 users/*/friends/{탈퇴유저} 삭제
                                val usersSnapshot = firestore.collection("users").get().await()
                                usersSnapshot.documents.forEach { userDoc ->
                                    firestore.collection("users")
                                        .document(userDoc.id)
                                        .collection("friends")
                                        .document(currentUserId)
                                        .delete()
                                        .await()
                                }

                                // 9. Realtime Database의 알림 삭제
                                FirebaseDatabase.getInstance().reference
                                    .child("notifications")
                                    .child(currentUserId)
                                    .removeValue()
                                    .await()

                                // 로그아웃 및 홈 이동
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    auth.signOut()
                                    android.widget.Toast.makeText(context, "회원 탈퇴가 완료되었습니다.", android.widget.Toast.LENGTH_LONG).show()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, "탈퇴 처리 중 오류: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing
                ) {
                    Text("확인", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

// 상세 정보 아이템 컴포저블
@Composable
fun DetailItem(label: String, value: String, valueTextAlign: TextAlign = TextAlign.Start) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val itemSpacing = (screenWidth.value * 0.04f).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = (screenHeight.value * 0.005f).dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = (screenWidth.value * 0.035f).sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(itemSpacing))
        Text(
            text = value,
            fontSize = (screenWidth.value * 0.035f).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = valueTextAlign,
            modifier = Modifier.widthIn(min = 0.dp, max = (screenWidth.value * 0.35f).dp)
        )
    }
}