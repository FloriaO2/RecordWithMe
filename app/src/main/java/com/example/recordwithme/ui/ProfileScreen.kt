package com.example.recordwithme.ui

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.FirebaseDatabase

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
                                            val requestData = mapOf(
                                                "fromUserId" to currentUserId,
                                                "fromUserName" to (currentUserEmail ?: currentUserId),
                                                "timestamp" to System.currentTimeMillis()
                                            )

                                            // Firestore에 친구 신청 저장
                                            firestore.collection("users")
                                                .document(user.id)
                                                .collection("friendRequests")
                                                .document(currentUserId)
                                                .set(requestData)
                                                .addOnSuccessListener {
                                                    // ✅ Realtime Database에도 저장 (알림용)
                                                    val realtimeDb = FirebaseDatabase.getInstance().reference
                                                    realtimeDb.child("notifications")
                                                        .child(user.id)  // 수신자 ID 경로
                                                        .push()
                                                        .setValue(requestData)

                                                    onDismiss()
                                                }
                                                .addOnFailureListener { e ->
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "친구 추가에 실패했습니다: ${e.message}",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
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

// 최종 ProfileScreen 컴포저블
@Composable
fun ProfileScreen(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser ?: return
    val currentUserId = currentUser.uid
    val currentUserEmail = currentUser.email

    var showFriendDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    val selectedFriendIds = remember { mutableStateListOf<String>() }
    var userName by remember { mutableStateOf("") }
    var userDisplayId by remember { mutableStateOf("") }
    var groupMode by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var groupNote by remember { mutableStateOf("") }

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
                val name = doc.getString("name") ?: return@mapNotNull null
                Friend(id, name, "친구")
            }
        } catch (_: Exception) {}
    }

    // 전체 화면 Box로 감싸서 버튼을 고정 위치에 띄우기
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 상단 프로필 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(profileAlpha)
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(25.dp))

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    val displayText = when {
                        userName.isNotEmpty() -> userName.first().uppercaseChar().toString()
                        userDisplayId.isNotEmpty() -> userDisplayId.first().uppercaseChar().toString()
                        currentUserEmail?.isNotEmpty() == true -> currentUserEmail.first().uppercaseChar().toString()
                        else -> "U"
                    }

                    Text(
                        text = displayText,
                        fontSize = 48.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.width(350.dp)
                ) {
                    Text("Edit Profile", color = Color.Black)
                }

            }

            // Friends 영역
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
                            border = BorderStroke(2.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.Person, contentDescription = "Add Friend", modifier = Modifier.size(25.dp), tint = Color.Black)
                            }
                        }

                        Button(
                            onClick = {
                                groupMode = !groupMode
                                if (!groupMode) selectedFriendIds.clear()
                            },
                            modifier = Modifier.width(70.dp).height(48.dp),
                            shape = RoundedCornerShape(10),
                            border = BorderStroke(2.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.Group, contentDescription = "Add Group", modifier = Modifier.size(28.dp), tint = Color.Black)
                            }
                        }
                    }
                }
            }

            if (friends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "친구가 없습니다.\n친구 추가 버튼을 눌러 친구를 추가해보세요.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        lineHeight = 30.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
                        }

                        FriendItem(
                            friend = friend,
                            isSelected = isSelected,
                            groupMode = groupMode,
                            onRemoveClick = { toRemove ->
                                firestore.collection("users").document(currentUserId)
                                    .collection("friends").document(toRemove.id).delete()
                                friends = friends.filter { it.id != toRemove.id }
                                selectedFriendIds.remove(toRemove.id)
                            }
                        )
                    }
                }
            }
        }

        // ✅ 하단 중앙 고정 버튼 (오버레이 방식, 배경 없음)
        if (groupMode && selectedFriendIds.isNotEmpty()) {
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
                        firestore.collection("groups").document(groupId).set(groupData).addOnSuccessListener {
                            groupMembers.forEach { memberId ->
                                val memberGroupData = groupData + ("groupId" to groupId)
                                firestore.collection("users").document(memberId).collection("groups").document(groupId).set(memberGroupData)
                            }
                            groupName = ""
                            groupNote = ""
                            selectedFriendIds.clear()
                            groupMode = false
                            showGroupDialog = false
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
    }
}