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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

// --- 데이터 클래스 ---
data class Friend(val id: String, val name: String, val mutual: String) {
    val initial: String get() = name.firstOrNull()?.toString() ?: ""
}
data class User(val id: String, val name: String, val loginType: String)

// --- 친구 아이템 ---
@Composable
fun FriendItem(friend: Friend, onRemoveClick: (Friend) -> Unit) {
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

// --- 친구 검색 다이얼로그 ---
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
                val querySnapshot = firestore.collection("users")
                    .get()
                    .await()

                searchResults = querySnapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val email = doc.getString("email")
                    val loginType = if (email != null) "google" else "normal"
                    val displayName = email ?: id
                    if (displayName.contains(searchText, ignoreCase = true) && id != currentUserId) {
                        User(id, displayName, loginType)
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
                    label = { Text("이메일 또는 ID로 검색") },
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
                                    Text(user.name, modifier = Modifier.weight(1f))
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

// --- 메인 프로필 스크린 ---
@Composable
fun ProfileScreen(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser ?: return
    val currentUserId = currentUser.uid
    val currentUserEmail = currentUser.email

    var showFriendDialog by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }

    // Firestore에서 친구 목록 불러오기
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
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 1.dp),
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
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.width(350.dp)
        ) {
            Text("Edit Profile", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(28.dp))
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
                    shape = RoundedCornerShape(10),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.width(70.dp).height(57.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF4F4F4F)
                    ),
                    border = BorderStroke(2.dp, Color(0xBC959596))
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.Person, contentDescription = "Add Friend", modifier = Modifier.size(25.dp))
                }

                Button(
                    onClick = { },
                    shape = RoundedCornerShape(10),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.width(70.dp).height(57.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF4F4F4F)
                    ),
                    border = BorderStroke(2.dp, Color(0xBC959596))
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.Group, contentDescription = "Add Group", modifier = Modifier.size(30.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.Start) {
            friends.forEach { friend ->
                FriendItem(friend = friend) { toRemove ->
                    firestore.collection("users")
                        .document(currentUserId)
                        .collection("friends")
                        .document(toRemove.id)
                        .delete()
                    friends = friends.filter { it.id != toRemove.id }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showFriendDialog) {
        FriendSearchDialog(
            currentUserId = currentUserId,
            currentUserEmail = currentUserEmail,
            firestore = firestore,
            onDismiss = { showFriendDialog = false },
            onFriendAdded = { newFriend -> friends = friends + newFriend }
        )
    }
}