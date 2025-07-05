package com.example.recordwithme.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Friend(val id: String, val name: String, val mutual: String) {
    val initial: String get() = name.firstOrNull()?.toString() ?: ""
}

data class User(val id: String, val name: String, val loginType: String, val displayId: String = "")

// FriendItem 컴포저블 추가
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

    LaunchedEffect(searchText) {
        if (searchText.isBlank()) {
            searchResults = emptyList()
        } else {
            loading = true
            try {
                val querySnapshot = firestore.collection("users").get().await()
                searchResults = querySnapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val userId = doc.getString("id")
                    val name = doc.getString("name")
                    val loginType = if (userId != null) "normal" else "google"
                    val displayName = name ?: userId ?: id
                    val displayId = userId ?: ""

                    if ((displayName.contains(searchText, ignoreCase = true) || displayId.contains(searchText, ignoreCase = true)) && id != currentUserId) {
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
                        Text("검색 결과 없음", color = Color.Gray)
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
                                        if (user.id.isNotEmpty()) {
                                            Text("@${user.id}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val friendData = mapOf("name" to user.name)
                                            firestore.collection("users")
                                                .document(currentUserId)
                                                .collection("friends")
                                                .document(user.id)
                                                .set(friendData)
                                                .addOnSuccessListener {
                                                    onFriendAdded(Friend(user.id, user.name, "새 친구"))
                                                    onDismiss()
                                                }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("+")
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

    // Firestore에서 사용자 정보 불러오기
    LaunchedEffect(currentUserId) {
        try {
            val userDoc = firestore.collection("users")
                .document(currentUserId)
                .get()
                .await()

            if (userDoc.exists()) {
                userName = userDoc.getString("name") ?: ""
                userDisplayId = userDoc.getString("id") ?: ""
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(currentUserId) {
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
    }

    // LazyColumn 상태 기억
    val listState = rememberLazyListState()

    // 스크롤에 따라 프로필 영역 투명도 조절 (0f ~ 1f)
    val profileAlpha by remember {
        derivedStateOf {
            val maxOffset = 300f
            val offset = listState.firstVisibleItemScrollOffset.toFloat()
            ((maxOffset - offset) / maxOffset).coerceIn(0f, 1f)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 프로필 영역 (스크롤 내리면 서서히 사라짐)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(profileAlpha)
                .padding(vertical = 16.dp),
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

            Spacer(modifier = Modifier.height(28.dp))
        }

        // sticky 효과를 위해 Box 로 감싸서 고정영역 따로 만들기
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
                        modifier = Modifier.width(90.dp).height(60.dp),
                        shape = RoundedCornerShape(10),
                        border = BorderStroke(2.dp, Color.Gray),
                        colors = ButtonDefaults.outlinedButtonColors(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Add Friend",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            groupMode = !groupMode
                            if (!groupMode) selectedFriendIds.clear()
                        },
                        modifier = Modifier.width(90.dp).height(60.dp),
                        shape = RoundedCornerShape(10),
                        border = BorderStroke(2.dp, Color.Gray),
                        colors = ButtonDefaults.outlinedButtonColors(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.Group,
                                contentDescription = "Add Group",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }
        }

        // 친구 목록만 LazyColumn으로 스크롤 가능하게
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 80.dp),
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

        // 그룹 추가 버튼: 흰 영역 없이 버튼만 띄우기
        if (groupMode && selectedFriendIds.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = { showGroupDialog = true },
                    modifier = Modifier
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
    }

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