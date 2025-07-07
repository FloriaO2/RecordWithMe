package com.example.recordwithme.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import androidx.compose.ui.platform.LocalContext

// 데이터 클래스들
data class Friend(val id: String, val name: String, val mutual: String) {
    val initial: String get() = name.firstOrNull()?.toString() ?: ""
}

data class User(val id: String, val name: String, val loginType: String, val displayId: String = "")

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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(0xFFBDBDBD)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = friend.initial,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(friend.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
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
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                    val loginType = if (userId.isNotEmpty()) "normal" else "google"
                    val displayName = name.ifBlank { userId.ifBlank { id } }
                    val displayId = userId

                    if ((displayName.contains(searchText, ignoreCase = true) ||
                                displayId.contains(searchText, ignoreCase = true)) &&
                        id != currentUserId &&
                        !currentFriendIds.contains(id)
                    ) {
                        User(id, displayName, loginType, displayId)
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
                    label = { Text("이름 또는 아이디로 검색") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

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
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.name, fontWeight = FontWeight.Medium)
                                        if (user.displayId.isNotEmpty()) {
                                            Text("@${user.displayId}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            // 이미 친구 요청을 보냈는지 확인
                                            firestore.collection("users")
                                                .document(user.id)
                                                .collection("friendRequests")
                                                .document(currentUserId)
                                                .get()
                                                .addOnSuccessListener { doc ->
                                                    if (doc.exists()) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "이미 친구 요청을 보냈습니다",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                        return@addOnSuccessListener
                                                    }

                                                    // 이미 친구인지 확인
                                                    firestore.collection("users")
                                                        .document(currentUserId)
                                                        .collection("friends")
                                                        .document(user.id)
                                                        .get()
                                                        .addOnSuccessListener { friendDoc ->
                                                            if (friendDoc.exists()) {
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "이미 친구입니다",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                                return@addOnSuccessListener
                                                            }

                                                            // 친구 요청 전송
                                                            val requestData = mapOf(
                                                                "fromUserId" to currentUserId,
                                                                "fromUserName" to (currentUserEmail ?: currentUserId),
                                                                "timestamp" to System.currentTimeMillis()
                                                            )

                                                            Log.d("ProfileScreen", "친구 요청 전송 시작: ${user.id}에게")

                                                            // Firestore에 친구 신청 저장
                                                            firestore.collection("users")
                                                                .document(user.id)
                                                                .collection("friendRequests")
                                                                .document(currentUserId)
                                                                .set(requestData)
                                                                .addOnSuccessListener {
                                                                    Log.d("ProfileScreen", "Firestore 친구 요청 저장 성공")

                                                                    // ✅ Realtime Database에도 저장 (알림용)
                                                                    val realtimeDb = FirebaseDatabase.getInstance().reference
                                                                    val notificationRef = realtimeDb.child("notifications").child(user.id)

                                                                    Log.d("ProfileScreen", "Realtime Database 경로: notifications/${user.id}")

                                                                    notificationRef.push().setValue(requestData)
                                                                        .addOnSuccessListener {
                                                                            Log.d("ProfileScreen", "Realtime Database 알림 저장 성공")
                                                                            android.widget.Toast.makeText(
                                                                                context,
                                                                                "친구 요청을 보냈습니다",
                                                                                android.widget.Toast.LENGTH_SHORT
                                                                            ).show()
                                                                            onDismiss()
                                                                        }
                                                                        .addOnFailureListener { e ->
                                                                            Log.e("ProfileScreen", "Realtime Database 저장 실패: ${e.message}")
                                                                            android.widget.Toast.makeText(
                                                                                context,
                                                                                "알림 저장에 실패했습니다: ${e.message}",
                                                                                android.widget.Toast.LENGTH_SHORT
                                                                            ).show()
                                                                        }
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    Log.e("ProfileScreen", "Firestore 친구 요청 저장 실패: ${e.message}")
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        "친구 추가에 실패했습니다: ${e.message}",
                                                                        android.widget.Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                        }
                                                }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("친구 신청", fontSize = 13.sp)
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

    // 프로필 이미지 이동 애니메이션
    val profileImageOffset by animateFloatAsState(
        targetValue = if (showDetails) -0.23f else 0f,
        animationSpec = tween(1000),
        label = "profileImageOffset"
    )

    // 프로필 이미지 세로 위치 조정 (위로 이동)
    val profileImageVerticalOffset = -50f

    // 프로필 텍스트 이동 애니메이션 (구글 로그인일 때는 고정, 일반 로그인일 때만 이동)
    val isGoogleLogin = !(currentUserEmail?.endsWith("@recordwith.me") == true)
    val profileTextOffset by animateFloatAsState(
        targetValue = if (showDetails && !isGoogleLogin) -0.23f else 0f,
        animationSpec = tween(1000),
        label = "profileTextOffset"
    )


    // Details 우측에서 등장 애니메이션
    val detailsOffset by animateFloatAsState(
        targetValue = if (showDetails) 0f else 3f,
        animationSpec = tween(1000),
        label = "detailsOffset"
    )

    // 사용자 정보 및 친구 목록 불러오기
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
            }

            val snapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .await()
            friends = snapshot.documents.mapNotNull { doc ->
                val id = doc.id

                // 친구의 사용자 문서에서 정보 가져오기
                val friendUserDoc = firestore.collection("users").document(id).get().await()
                val name = friendUserDoc.getString("name") ?: ""
                val email = friendUserDoc.getString("email") ?: ""

                // 1순위: name이 설정되어 있고 비어있지 않은 경우 name 사용
                // 2순위: name이 없거나 비어있는 경우 이메일 사용 (@gmail.com 제거)
                val displayName = if (name.isNotBlank()) {
                    name
                } else {
                    if (email.endsWith("@gmail.com")) {
                        email.removeSuffix("@gmail.com")
                    } else {
                        email
                    }
                }

                Friend(id, displayName, "친구")
            }
            friendsLoaded = true
        } catch (_: Exception) {
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
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile 텍스트는 항상 상단 중앙에 고정
                    Text(
                        "Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                    )

                    // 프로필과 상세 정보를 포함하는 Box
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // 좌측: 프로필 이미지 (중앙에서 좌측으로 이동)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset(
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
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0E0E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                val displayText = when {
                                    userName.isNotEmpty() -> userName.first().uppercaseChar()
                                        .toString()

                                    userDisplayId.isNotEmpty() -> userDisplayId.first()
                                        .uppercaseChar().toString()

                                    currentUserEmail?.isNotEmpty() == true -> currentUserEmail.first()
                                        .uppercaseChar().toString()

                                    else -> "U"
                                }

                                Text(
                                    text = displayText,
                                    fontSize = 48.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
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
                            Spacer(modifier = Modifier.height(160.dp)) // 이미지 높이 + 간격 (12dp 추가)

                            val displayName = when {
                                userName.isNotEmpty() -> userName
                                userDisplayId.isNotEmpty() -> userDisplayId
                                currentUserEmail?.isNotEmpty() == true -> currentUserEmail
                                else -> currentUserId
                            }

                            Text(displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            if (userDisplayId.isNotEmpty()) {
                                Text("@$userDisplayId", fontSize = 16.sp, color = Color.Gray)
                            }
                        }

                        // 우측: 상세 정보 (화면 바깥에서 들어옴)
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 24.dp)
                                .width(200.dp)
                                .offset(
                                    x = with(LocalDensity.current) {
                                        (detailsOffset * 300).toDp()
                                    },
                                    y = if (isGoogleLogin) (-11).dp else 20.dp
                                )
                        ) {
                            // 상세 정보 내용
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
                            ) {
                                // 구글 로그인인지 확인 (@recordwith.me가 아닌 경우 모두 구글 로그인)
                                val isGoogleLogin =
                                    !(currentUserEmail?.endsWith("@recordwith.me") == true)

                                if (isGoogleLogin) {
                                    // 구글 로그인: 이메일/친구수만 표시
                                    val email = currentUserEmail ?: "설정되지 않음"
                                    if (email != "설정되지 않음" && email.contains("@")) {
                                        val parts = email.split("@")
                                        DetailItem("이메일", "${parts[0]}\n@${parts[1]}")
                                    } else {
                                        DetailItem("이메일", email)
                                    }
                                    DetailItem("친구 수", "${friends.size}명")
                                } else {
                                    // 일반 로그인: 이름/아이디/친구수만 표시
                                    DetailItem("이름", userName.ifEmpty { "설정되지 않음" })
                                    DetailItem("아이디", userDisplayId.ifEmpty { "설정되지 않음" })
                                    DetailItem("친구 수", "${friends.size}명")
                                }

                                // 회원 탈퇴 버튼
                                TextButton(
                                    onClick = {
                                        // 회원 탈퇴 로직 구현
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = (-12).dp, x = 12.dp)
                                ) {
                                    Text(
                                        "회원 탈퇴",
                                        color = Color(0xFF8B0000), // 검붉은 색
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 버튼은 항상 같은 위치와 너비로 고정
                    Button(
                        onClick = { showDetails = !showDetails },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.width(350.dp)
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
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                    )

                    Spacer(modifier = Modifier.height(100.dp))

                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
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
                        .height(80.dp)
                        .padding(horizontal = 40.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Friends", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showFriendDialog = true },
                                modifier = Modifier.width(70.dp).height(48.dp),
                                shape = RoundedCornerShape(10),
                                border = BorderStroke(
                                    2.dp,
                                    if (showFriendDialog) Color(0xFF424242) else Color.Gray
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (showFriendDialog) Color(0x59282828) else Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "+",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showFriendDialog) Color(0xFF424242) else Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = "Add Friend",
                                        modifier = Modifier.size(25.dp),
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
                                modifier = Modifier.width(70.dp).height(48.dp),
                                shape = RoundedCornerShape(10),
                                border = BorderStroke(
                                    2.dp,
                                    if (groupMode) Color(0xBF424242) else Color.Gray
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (groupMode) Color(0x59282828) else Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "+",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (groupMode) Color(0xFF424242) else Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Group,
                                        contentDescription = "Add Group",
                                        modifier = Modifier.size(28.dp),
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
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (groupMode) {
                            "그룹을 만들기 위해서는 먼저 친구를 추가해주세요.\n친구 추가 버튼을 눌러 친구를 추가해보세요."
                        } else {
                            "친구가 없습니다.\n친구 추가 버튼을 눌러 친구를 추가해보세요."
                        },
                        color = Color.Gray,
                        fontSize = 14.sp,
                        lineHeight = 30.sp,
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
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(friends) { friend ->
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
                                    // 양쪽에서 친구 관계 삭제
                                    firestore.collection("users")
                                        .document(currentUserId)
                                        .collection("friends")
                                        .document(toRemove.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            // 상대방의 친구 목록에서도 삭제
                                            firestore.collection("users")
                                                .document(toRemove.id)
                                                .collection("friends")
                                                .document(currentUserId)
                                                .delete()
                                                .addOnSuccessListener {
                                                    // UI 업데이트
                                                    friends =
                                                        friends.filter { it.id != toRemove.id }
                                                    selectedFriendIds.remove(toRemove.id)
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "${toRemove.name}님을 친구 목록에서 삭제했습니다",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "친구 삭제 중 오류가 발생했습니다: ${e.message}",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            android.widget.Toast.makeText(
                                                context,
                                                "친구 삭제 중 오류가 발생했습니다: ${e.message}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            )
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
                            .padding(bottom = 24.dp)
                            .wrapContentWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF90CAF9),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text("그룹 추가하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = groupNote,
                                onValueChange = { groupNote = it },
                                label = { Text("메모") }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (groupName.isBlank()) return@TextButton
                            val groupMembers = selectedFriendIds + currentUserId
                            val groupId = groupName.trim()
                            val groupData = mapOf(
                                "name" to groupName,
                                "note" to groupNote,
                                "members" to groupMembers.toList(),
                                "creator" to currentUserId
                            )
                            firestore.collection("groups").document(groupId).set(groupData)
                                .addOnSuccessListener {
                                    groupMembers.forEach { memberId ->
                                        val memberGroupData = groupData + ("groupId" to groupId)
                                        firestore.collection("users").document(memberId)
                                            .collection("groups").document(groupId)
                                            .set(memberGroupData)
                                    }
                                    groupName = ""
                                    groupNote = ""
                                    selectedFriendIds.clear()
                                    GroupModeState.isGroupMode = false
                                    showGroupDialog = false
                                    navController.popBackStack() // 그룹 생성 후 GroupScreen으로 돌아가기
                                }
                        }) {
                            Text("완료")
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
        }
    }
}

// 상세 정보 아이템 컴포저블
@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}