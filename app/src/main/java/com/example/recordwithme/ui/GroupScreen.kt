package com.example.recordwithme.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// 데이터 모델
data class UserGroup(
    val id: String = "",
    val name: String = "",
    val membersCount: Int = 0,
    val note: String = "",
    val creator: String = "",
    val members: List<String> = emptyList()
)

@Composable
fun GroupScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val groups = remember { mutableStateListOf<UserGroup>() }
    val isLoading = remember { mutableStateOf(true) }
    val friends = remember { mutableStateListOf<String>() }

    // 아코디언 패널 상태
    var selectedGroupId by remember { mutableStateOf<String?>(null) }

    // 화면 크기에 따른 반응형 설정
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Firestore에서 데이터 가져오기
    LaunchedEffect(true) {
        try {
            // 현재 사용자 ID 가져오기
            val auth = FirebaseAuth.getInstance()
            val currentUserId = auth.currentUser?.uid ?: return@LaunchedEffect

            // 친구 목록 가져오기
            val friendsSnapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .await()
            friends.clear()
            friends.addAll(friendsSnapshot.documents.map { it.id })

            // 그룹 목록 가져오기
            val snapshot = firestore.collection("groups").get().await()
            groups.clear()
            for (document in snapshot.documents) {
                val groupId = document.id
                val groupName = document.getString("name") ?: ""
                val note = document.getString("note") ?: ""
                val creator = document.getString("creator") ?: ""
                val members = document.get("members") as? List<String> ?: emptyList()
                val membersCount = members.size

                val group = UserGroup(
                    id = groupId,
                    name = groupName,
                    membersCount = membersCount,
                    note = note,
                    creator = creator,
                    members = members
                )
                groups.add(group)
            }
        } catch (e: Exception) {
            println("Error fetching data: $e")
        }
        isLoading.value = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding((screenWidth.value * 0.06f).dp)
        ) {
            Spacer(modifier = Modifier.width((screenWidth.value * 0.04f).dp))
            Text(
                text = "My Groups",
                style = TextStyle(
                    fontSize = (screenWidth.value * 0.06f).sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = (screenHeight.value * 0.02f).dp)
            )

            if (isLoading.value) {
                Text(text = "Loading...")
            } else {
                if (groups.isEmpty()) {
                    Spacer(modifier = Modifier.padding((screenHeight.value * 0.02f).dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy((screenHeight.value * 0.005f).dp)
                    ) {
                        Text(
                            text = "그룹이 없습니다.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 0.5f
                            )
                        )
                        Spacer(modifier = Modifier.height((screenHeight.value * 0.002f).dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "지금 바로 그룹을 만들어보세요!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.Gray,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 0.5f
                                )
                            )
                            Spacer(modifier = Modifier.padding((screenWidth.value * 0.01f).dp))
                            Text(
                                text = "그룹 만들기",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5f
                                ),
                                modifier = Modifier.clickable {
                                    if (friends.isEmpty()) {
                                        GroupModeState.isGroupMode = true
                                        navController.navigate("profile") {
                                            launchSingleTop = true
                                        }
                                    } else {
                                        GroupModeState.isGroupMode = true
                                        navController.navigate("profile") {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    groups.forEach { group ->
                        GroupItem(
                            group = group,
                            isExpanded = selectedGroupId == group.id,
                            onClick = {
                                selectedGroupId = if (selectedGroupId == group.id) null else group.id
                            },
                            onDeleteGroup = { groupToDelete ->
                                firestore.collection("groups").document(groupToDelete.id).delete()
                                groups.remove(groupToDelete)
                                if (selectedGroupId == groupToDelete.id) {
                                    selectedGroupId = null
                                }
                            },
                            screenWidth = screenWidth,
                            screenHeight = screenHeight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // FloatingActionButton
        FloatingActionButton(
            onClick = {
                if (friends.isEmpty()) {
                    GroupModeState.isGroupMode = true
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                } else {
                    GroupModeState.isGroupMode = true
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = (screenWidth.value * 0.075f).dp,
                    bottom = (screenHeight.value * 0.05f).dp
                ),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Group")
        }
    }
}

@Composable
fun GroupItem(
    group: UserGroup,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onDeleteGroup: (UserGroup) -> Unit,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp
) {
    val configuration = LocalConfiguration.current
    val localScreenWidth = configuration.screenWidthDp.dp
    val localScreenHeight = configuration.screenHeightDp.dp

    // 통합된 카드 컨테이너
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = (localScreenWidth.value * 0.02f).dp, vertical = (localScreenHeight.value * 0.005f).dp),
        shape = RoundedCornerShape((localScreenWidth.value * 0.02f).dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Color(0xFFF5F5F5) else Color.Transparent
        ),
        elevation = if (isExpanded) CardDefaults.cardElevation(defaultElevation = (localScreenWidth.value * 0.005f).dp) else CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // 그룹 아이템 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(vertical = (localScreenHeight.value * 0.01f).dp, horizontal = (localScreenWidth.value * 0.02f).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size((localScreenWidth.value * 0.1f).dp)
                        .clip(RoundedCornerShape((localScreenWidth.value * 0.02f).dp))
                        .background(Color.Gray)
                )
                Spacer(modifier = Modifier.width((localScreenWidth.value * 0.04f).dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = (localScreenWidth.value * 0.04f).sp,
                            fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isExpanded) MaterialTheme.colorScheme.primary else Color.Black
                    )
                    Text(
                        text = "${group.membersCount} members",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Gray,
                            fontSize = (localScreenWidth.value * 0.035f).sp
                        )
                    )
                }
            }

            // 확장된 상세 정보 (공백 없이 연결)
            if (isExpanded) {
                GroupDetailPanel(
                    group = group,
                    onDeleteGroup = onDeleteGroup,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    isExpanded = isExpanded
                )
            }
        }
    }
}

@Composable
fun GroupDetailPanel(
    group: UserGroup,
    onDeleteGroup: (UserGroup) -> Unit,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean
) {
    val panelHeight by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "panelHeight"
    )

    // 상세정보 컨텐츠
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = (screenWidth.value * 0.02f).dp,
                end = (screenWidth.value * 0.02f).dp,
                top = (screenHeight.value * 0f).dp,
                bottom = (screenHeight.value * 0.005f).dp
            )
    ) {
                // 메모
                if (group.note.isNotEmpty()) {
                    Text(
                        text = "메모",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (screenWidth.value * 0.04f).sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = (screenHeight.value * 0.01f).dp)
                    )
                    Text(
                        text = group.note,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (screenWidth.value * 0.035f).sp,
                            color = Color.Gray
                        ),

                    )
                }

                // 멤버 정보
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "멤버 (${group.membersCount}명)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (screenWidth.value * 0.04f).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    TextButton(
                        onClick = { /* 멤버 추가 기능 */ },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.padding(end = (screenWidth.value * 0.03f).dp)  // 오른쪽 여백 추가로 더 왼쪽으로 이동
                    ) {
                        Text(
                            text = "멤버 추가하기",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = (screenWidth.value * 0.03f).sp,
                                color = Color.Gray  // 회색으로 변경
                            )
                        )
                    }
                }

                                // 멤버 목록 (사용자 정보 표시)
                Column(
                    verticalArrangement = Arrangement.spacedBy((screenHeight.value * 0.003f).dp)
                ) {
                    group.members.take(5).forEach { memberId ->
                        MemberItem(
                            memberId = memberId,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight
                        )
                    }

                    if (group.members.size > 5) {
                        Text(
                            text = "... 외 ${group.members.size - 5}명",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (screenWidth.value * 0.035f).sp,
                                color = Color.Gray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height((screenHeight.value * 0.005f).dp))

                // 액션 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((screenWidth.value * 0.02f).dp)
                ) {
                    Button(
                        onClick = { /* 편집 기능 */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape((screenWidth.value * 0.02f).dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size((screenWidth.value * 0.04f).dp)
                        )
                        Spacer(modifier = Modifier.width((screenWidth.value * 0.02f).dp))
                        Text(
                            text = "편집",
                            fontSize = (screenWidth.value * 0.035f).sp
                        )
                    }

                    Button(
                        onClick = { onDeleteGroup(group) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ),
                        shape = RoundedCornerShape((screenWidth.value * 0.02f).dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size((screenWidth.value * 0.04f).dp)
                        )
                        Spacer(modifier = Modifier.width((screenWidth.value * 0.02f).dp))
                        Text(
                            text = "삭제",
                            fontSize = (screenWidth.value * 0.035f).sp
                        )
                    }
                }
            }
        }


@Composable
fun MemberItem(
    memberId: String,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp
) {
    var memberName by remember { mutableStateOf("") }
    var memberEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Firestore에서 사용자 정보 가져오기
    LaunchedEffect(memberId) {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userDoc = firestore.collection("users").document(memberId).get().await()

            if (userDoc.exists()) {
                memberName = userDoc.getString("name") ?: ""
                memberEmail = userDoc.getString("email") ?: ""
            }
        } catch (e: Exception) {
            println("Error fetching user info: $e")
        }
        isLoading = false
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = (screenHeight.value * 0.005f).dp)
    ) {
        Icon(
            Icons.Filled.Person,
            contentDescription = "Member",
            modifier = Modifier.size((screenWidth.value * 0.04f).dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width((screenWidth.value * 0.02f).dp))

        if (isLoading) {
            Text(
                text = "로딩 중...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (screenWidth.value * 0.035f).sp,
                    color = Color.Gray
                )
            )
        } else {
            val displayText = when {
                memberEmail.endsWith("@recordwith.me") && memberName.isNotEmpty() -> memberName
                memberEmail.isNotEmpty() -> memberEmail
                else -> memberId
            }

            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (screenWidth.value * 0.035f).sp,
                    color = Color.Gray
                )
            )
        }
    }
}

