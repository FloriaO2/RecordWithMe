package com.example.recordwithme.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun FriendItem(friend: Friend, isSelected: Boolean = false, onRemoveClick: (Friend) -> Unit) {
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

        IconButton(onClick = { showConfirmDialog = true }) {
            Icon(Icons.Filled.Close, contentDescription = "Remove")
        }
    }
}

@Composable
fun FriendSearchDialog(
    currentUserId: String,
    currentUserEmail: String?,
    firestore: FirebaseFirestore,
    onDismiss: () -> Unit,
    onFriendAdded: (Friend) -> Unit
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
    var selectedFriendIds by remember { mutableStateOf(mutableSetOf<String>()) }

    var groupMode by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var groupNote by remember { mutableStateOf("") }

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

    Column(
        modifier = Modifier.fillMaxSize(),
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
            Text(
                text = (currentUserEmail ?: currentUserId).first().uppercaseChar().toString(),
                fontSize = 48.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(currentUserEmail ?: currentUserId, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { /* 프로필 수정 예정 */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.width(350.dp)
        ) {
            Text("Edit Profile", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Friends 상단
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, bottom = 12.dp, end = 24.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.Person, contentDescription = "Add Friend", modifier = Modifier.size(32.dp))
                    }
                }

                Button(
                    onClick = {
                        if (groupMode && selectedFriendIds.isNotEmpty()) {
                            showGroupDialog = true
                        } else {
                            groupMode = !groupMode
                            selectedFriendIds.clear()
                        }
                    },
                    modifier = Modifier.width(90.dp).height(60.dp),
                    shape = RoundedCornerShape(10),
                    border = BorderStroke(2.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.Group, contentDescription = "Add Group", modifier = Modifier.size(34.dp))
                    }
                }
            }
        }

        // Friends 리스트
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            friends.forEach { friend ->
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
                        onRemoveClick = { toRemove ->
                            firestore.collection("users").document(currentUserId)
                                .collection("friends").document(toRemove.id).delete()
                            friends = friends.filter { it.id != toRemove.id }
                            selectedFriendIds.remove(toRemove.id)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                    if (groupName.isBlank()) {
                        // 그룹 이름 필수 체크
                        return@TextButton
                    }
                    val groupMembers = selectedFriendIds + currentUserId
                    val groupId = groupName.trim() // 그룹 이름을 ID로 사용 (공백 제거)
                    val groupData = mapOf(
                        "name" to groupName,
                        "note" to groupNote,
                        "members" to groupMembers.toList(),
                        "creator" to currentUserId
                    )
                    firestore.collection("groups")
                        .document(groupId)
                        .set(groupData)
                        .addOnSuccessListener {
                            // 멤버별 groups 컬렉션에도 저장
                            groupMembers.forEach { memberId ->
                                val memberGroupData = mapOf(
                                    "groupId" to groupId,
                                    "name" to groupName,
                                    "note" to groupNote,
                                    "creator" to currentUserId,
                                    "members" to groupMembers.toList()
                                )
                                firestore.collection("users")
                                    .document(memberId)
                                    .collection("groups")
                                    .document(groupId)
                                    .set(memberGroupData)
                            }
                            // 저장 후 상태 초기화 및 다이얼로그 닫기
                            groupName = ""
                            groupNote = ""
                            selectedFriendIds.clear()
                            groupMode = false
                            showGroupDialog = false
                        }
                        .addOnFailureListener { e ->
                            // 실패 처리(원한다면)
                        }
                }) {
                    Text("완료")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // 다이얼로그 닫을 때 상태 초기화도 가능
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
            onFriendAdded = { newFriend ->
                friends = friends + newFriend
            }
        )
    }
}