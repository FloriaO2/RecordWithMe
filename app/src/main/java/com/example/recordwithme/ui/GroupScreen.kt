package com.example.recordwithme.ui

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val groups = remember { mutableStateListOf<UserGroup>() }
    val isLoading = remember { mutableStateOf(true) }
    val friends = remember { mutableStateListOf<String>() }

    // 현재 사용자 ID 가져오기
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    // 아코디언 패널 상태
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    
    // 다이얼로그 상태
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var groupToLeave by remember { mutableStateOf<UserGroup?>(null) }

    // 화면 크기에 따른 반응형 설정
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Firestore에서 데이터 가져오기
    LaunchedEffect(true) {
        try {
            // 현재 사용자 ID 확인
            if (currentUserId == null) {
                println("GroupScreen: currentUserId is null in LaunchedEffect")
                return@LaunchedEffect
            }
            
            // 친구 목록 가져오기
            val friendsSnapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .await()
            friends.clear()
            friends.addAll(friendsSnapshot.documents.map { it.id })
            
            // 본인 그룹 목록만 가져오기
            val userGroupsSnapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("groups")
                .get()
                .await()
            groups.clear()
            for (document in userGroupsSnapshot.documents) {
                val groupId = document.getString("groupId") ?: document.id
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding((screenWidth.value * 0.06f).dp)
        ) {
            item {
                Spacer(modifier = Modifier.width((screenWidth.value * 0.04f).dp))
            Text(
                text = "My Groups",
                    style = TextStyle(
                        fontSize = (screenWidth.value * 0.06f).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = (screenHeight.value * 0.02f).dp)
                )
            }
            if (isLoading.value) {
                item { Text(text = "Loading...") }
            } else {
                if (groups.isEmpty()) {
                    item {
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
                    }
                } else {
                    items(groups) { group ->
                        GroupItem(
                            group = group,
                            isExpanded = selectedGroupId == group.id,
                            onClick = {
                                selectedGroupId = if (selectedGroupId == group.id) null else group.id
                            },
                            onDeleteGroup = { groupToDelete ->
                                val groupDocRef = FirebaseFirestore.getInstance().collection("groups").document(groupToDelete.id)
                                groupDocRef.get().addOnSuccessListener { doc ->
                                    val members = doc.get("members") as? List<String> ?: groupToDelete.members
                                    
                                    // 먼저 그룹 캘린더의 모든 이미지 삭제
                                    FirebaseFirestore.getInstance().collection("groups")
                                        .document(groupToDelete.id)
                                        .collection("photos")
                                        .get()
                                        .addOnSuccessListener { photosSnapshot ->
                                            val deletePhotoTasks = photosSnapshot.documents.map { photoDoc ->
                                                photoDoc.reference.delete()
                                            }
                                            
                                            // 모든 이미지 삭제 완료 후 그룹 관련 데이터 삭제
                                            com.google.android.gms.tasks.Tasks.whenAll(deletePhotoTasks)
                                                .addOnSuccessListener {
                                                    android.util.Log.i("GroupDelete", "그룹 캘린더 이미지 삭제 완료")
                                                    
                                                    // 각 멤버의 users/{userId}/groups/{groupId} 삭제
                                                    val deleteMemberTasks = members.map { memberId ->
                                                        FirebaseFirestore.getInstance().collection("users")
                                                            .document(memberId)
                                                            .collection("groups")
                                                            .document(groupToDelete.id)
                                                            .delete()
                                                    }
                                                    
                                                    // 모든 멤버 그룹 참조 삭제 완료 후 그룹 문서 삭제
                                                    com.google.android.gms.tasks.Tasks.whenAll(deleteMemberTasks)
                                                        .addOnSuccessListener {
                                                            android.util.Log.i("GroupDelete", "모든 멤버 그룹 참조 삭제 완료")
                                                            
                                                            // 마지막으로 그룹 문서 삭제
                                                            FirebaseFirestore.getInstance().collection("groups")
                                                                .document(groupToDelete.id)
                                                                .delete()
                                                                .addOnSuccessListener {
                                                                    android.util.Log.i("GroupDelete", "그룹 문서 삭제 성공")
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        "${groupToDelete.name} 그룹이 완전히 삭제되었습니다",
                                                                        android.widget.Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    
                                                                    // UI에서 그룹 제거
                                                                    groups.remove(groupToDelete)
                                                                    if (selectedGroupId == groupToDelete.id) {
                                                                        selectedGroupId = null
                                                                    }
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    android.util.Log.e("GroupDelete", "그룹 문서 삭제 실패: ${e.message}")
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        "그룹 문서 삭제 실패: ${e.message}",
                                                                        android.widget.Toast.LENGTH_LONG
                                                                    ).show()
                                                                }
                                                        }
                                                        .addOnFailureListener { e ->
                                                            android.util.Log.e("GroupDelete", "멤버 그룹 참조 삭제 실패: ${e.message}")
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "멤버 그룹 참조 삭제 실패: ${e.message}",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                }
                                                .addOnFailureListener { e ->
                                                    android.util.Log.e("GroupDelete", "그룹 캘린더 이미지 삭제 실패: ${e.message}")
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "그룹 캘린더 이미지 삭제 실패: ${e.message}",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            android.util.Log.e("GroupDelete", "그룹 캘린더 이미지 조회 실패: ${e.message}")
                                            android.widget.Toast.makeText(
                                                context,
                                                "그룹 캘린더 이미지 조회 실패: ${e.message}",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }.addOnFailureListener { e ->
                                    android.util.Log.e("GroupDelete", "Failed to fetch group doc: ${e.message}")
                                    android.widget.Toast.makeText(
                                        context,
                                        "그룹 정보 조회 실패: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onLeaveGroup = { groupToLeave ->
                                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                                if (currentUserId != null) {
                                    // 본인만 users/{myId}/groups/{groupId} 삭제
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(currentUserId)
                                        .collection("groups")
                                        .document(groupToLeave.id)
                                        .delete()
                                    // groups/{groupId}의 members에서 본인 id만 제거
                                    val groupDocRef = FirebaseFirestore.getInstance().collection("groups").document(groupToLeave.id)
                                    groupDocRef.get().addOnSuccessListener { doc ->
                                        val members = (doc.get("members") as? List<String>)?.toMutableList() ?: groupToLeave.members.toMutableList()
                                        members.remove(currentUserId)
                                        
                                        if (members.isEmpty()) {
                                            // 멤버가 없으면 그룹 완전 삭제
                                            FirebaseFirestore.getInstance().collection("groups")
                                                .document(groupToLeave.id)
                                                .delete()
                                                .addOnSuccessListener {
                                                    // 그룹 캘린더의 사진들 삭제
                                                    FirebaseFirestore.getInstance().collection("groups")
                                                        .document(groupToLeave.id)
                                                        .collection("photos")
                                                        .get()
                                                        .addOnSuccessListener { photosSnapshot ->
                                                            photosSnapshot.documents.forEach { photoDoc ->
                                                                FirebaseFirestore.getInstance().collection("groups")
                                                                    .document(groupToLeave.id)
                                                                    .collection("photos")
                                                                    .document(photoDoc.id)
                                                                    .delete()
                                                            }
                                                        }
                                                    
                                                    // 모든 사용자의 개인 그룹 목록에서도 해당 그룹 삭제
                                                    FirebaseFirestore.getInstance().collection("users").get()
                                                        .addOnSuccessListener { usersSnapshot ->
                                                            usersSnapshot.documents.forEach { userDoc ->
                                                                FirebaseFirestore.getInstance().collection("users")
                                                                    .document(userDoc.id)
                                                                    .collection("groups")
                                                                    .document(groupToLeave.id)
                                                                    .delete()
                                                            }
                                                        }
                                                }
                                        } else {
                                            // 멤버가 있으면 멤버 목록만 업데이트
                                            FirebaseFirestore.getInstance().collection("groups").document(groupToLeave.id)
                                                .update("members", members)
                                        }
                                    }
                                    if (selectedGroupId == groupToLeave.id) {
                                        selectedGroupId = null
                                    }
                                    groups.removeAll { it.id == groupToLeave.id }
                                }
                            },
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            navController = navController
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height((screenHeight.value * 0.05f).dp)) }
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
    onLeaveGroup: (UserGroup) -> Unit,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp,
    navController: NavController
) {
    val firestore = FirebaseFirestore.getInstance()
    val groupId = group.id

    var membersCount by remember { mutableStateOf(group.membersCount) }

    // 실시간 구독
    DisposableEffect(groupId) {
        val listener = FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val newMembers = snapshot.get("members") as? List<String> ?: emptyList()
                    membersCount = newMembers.size
                }
            }
        onDispose { listener.remove() }
    }

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
                        text = "${membersCount} members",
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
                    onLeaveGroup = onLeaveGroup,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    isExpanded = isExpanded,
                    navController = navController
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailPanel(
    group: UserGroup,
    onDeleteGroup: (UserGroup) -> Unit,
    onLeaveGroup: (UserGroup) -> Unit,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp,
    isExpanded: Boolean,
    navController: NavController
) {
    val firestore = FirebaseFirestore.getInstance()
    val groupId = group.id
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isCreator = group.creator == currentUserId

    // 멤버 추가 다이얼로그 상태
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // (id, name)
    var selectedFriends by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loadingFriends by remember { mutableStateOf(false) }

    // 멤버 추가 버튼 클릭 시 친구 목록 불러오기
    fun loadFriendsNotInGroup() {
        loadingFriends = true
        if (currentUserId == null) return
        FirebaseFirestore.getInstance().collection("users").document(currentUserId).collection("friends").get()
            .addOnSuccessListener { snapshot ->
                val allFriends = snapshot.documents.map { it.id to (it.getString("name") ?: it.id) }
                // 그룹에 없는 친구만 필터링
                val notInGroup = allFriends.filter { (id, _) -> id !in group.members }
                friends = notInGroup
                loadingFriends = false
            }
            .addOnFailureListener {
                friends = emptyList()
                loadingFriends = false
            }
    }

    val panelHeight by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "panelHeight"
    )

    var members by remember { mutableStateOf(group.members) }
    var membersCount by remember { mutableStateOf(group.membersCount) }

    // 실시간 구독
    DisposableEffect(groupId) {
        val listener = FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val newMembers = snapshot.get("members") as? List<String> ?: emptyList()
                    members = newMembers
                    membersCount = newMembers.size
                }
            }
        onDispose { listener.remove() }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    // 상세정보 컨텐츠 (verticalScroll 제거)
    val targetWidth = if (isExpanded) screenWidth * 0.95f else 0.dp
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = 500), label = "panelWidth"
    )

    if (animatedWidth > 1.dp) {
        Box(
            modifier = Modifier
                .width(animatedWidth)
                .clip(RoundedCornerShape(0.dp))
                .padding(
                    start = (screenWidth.value * 0.02f).dp,
                    end = (screenWidth.value * 0.02f).dp,
                    top = (screenHeight.value * 0f).dp,
                    bottom = (screenHeight.value * 0.005f).dp
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 메모
                if (group.note.isNotEmpty()) {
                    Text(
                        text = "메모",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (screenWidth.value * 0.04f).sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = (screenHeight.value * 0.03f).dp, start = (screenWidth.value * 0.02f).dp)
                    )
                    Text(
                        text = group.note,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (screenWidth.value * 0.035f).sp,
                            color = Color.Gray
                        ),
                        modifier = Modifier.padding(start = (screenWidth.value * 0.02f).dp)
                    )
                }

                // 멤버 정보
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (screenWidth.value * 0.035f).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "멤버 (${membersCount}명)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (screenWidth.value * 0.04f).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    TextButton(
                        onClick = {
                            loadFriendsNotInGroup()
                            showAddMemberDialog = true
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.padding(end = (screenWidth.value * 0.035f).dp)
                    ) {
                        Text(
                            text = "멤버 추가하기",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (screenWidth.value * 0.03f).sp,
                                color = Color.Gray
                            )
                        )
                    }
                }

                // 멤버 목록 (사용자 정보 표시)
                Column(
                    verticalArrangement = Arrangement.spacedBy((screenHeight.value * 0.003f).dp),
                            modifier = Modifier.padding(start = (screenWidth.value * 0.02f).dp)
                ) {
                    members.take(5).forEach { memberId ->
                        MemberItem(
                            memberId = memberId,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight
                        )
                    }

                    if (members.size > 5) {
                        Text(
                            text = "... 외 ${members.size - 5}명",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (screenWidth.value * 0.035f).sp,
                                color = Color.Gray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height((screenHeight.value * 0.005f).dp))

                // 액션 버튼들
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy((screenHeight.value * 0.003f).dp)
                ) {
                    TextButton(
                        onClick = { /* 편집 기능 */ },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size((screenWidth.value * 0.04f).dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width((screenWidth.value * 0.02f).dp))
                        Text(
                            text = "편집",
                            fontSize = (screenWidth.value * 0.035f).sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                        TextButton(
                            onClick = {
                                showLeaveDialog = true
                            },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Leave",
                                modifier = Modifier.size((screenWidth.value * 0.04f).dp),
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.width((screenWidth.value * 0.02f).dp))
                            Text(
                                text = "탈퇴",
                                fontSize = (screenWidth.value * 0.035f).sp,
                                color = Color.Red
                            )
                        }

                    TextButton(
                        onClick = {
                            val intent = Intent(context, com.example.recordwithme.ui.GroupCalendarActivity::class.java)
                            intent.putExtra("groupId", group.id)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person, // 캘린더 아이콘으로 교체 가능
                            contentDescription = "Group Calendar",
                            modifier = Modifier.size((screenWidth.value * 0.04f).dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width((screenWidth.value * 0.02f).dp))
                        Text(
                            text = "그룹 캘린더",
                            fontSize = (screenWidth.value * 0.035f).sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }

    // 멤버 추가 다이얼로그
    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("그룹에 초대할 친구 선택") },
            text = {
                if (loadingFriends) {
                    Text("불러오는 중...")
                } else if (friends.isEmpty()) {
                    Text("추가할 수 있는 친구가 없습니다.")
                } else {
                    Column {
                        friends.forEach { (id, name) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedFriends.contains(id),
                                    onCheckedChange = { checked ->
                                        selectedFriends = if (checked) selectedFriends + id else selectedFriends - id
                                    }
                                )
                                // @recordwith.me면 name(있을 때만), 아니면 이메일 전체
                                val isRecordWithMe = id.endsWith("@recordwith.me")
                                val displayText = if (isRecordWithMe && name.isNotEmpty()) {
                                    name
                                } else {
                                    id
                                }
                                Text(text = displayText)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 코루틴 스코프에서 비동기 작업 실행
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                // 그룹 초대 로직 (프로필과 동일)
                                val groupName = group.name
                                val groupNote = group.note
                                val groupCreator = group.creator
                                val groupMembers = group.members
                                val groupId = group.id
                                val groupData = mapOf(
                                    "groupId" to groupId,
                                    "name" to groupName,
                                    "note" to groupNote,
                                    "creator" to groupCreator,
                                    "members" to groupMembers
                                )
                                // 현재 사용자 정보를 Firestore에서 가져오기
                                if (currentUserId == null) {
                                    println("GroupScreen: currentUserId is null")
                                    return@launch
                                }
                                
                                val currentUserDoc = FirebaseFirestore.getInstance().collection("users").document(currentUserId).get().await()
                                val currentUserName = currentUserDoc.getString("name") ?: ""
                                val currentUserEmail = currentUserDoc.getString("email") ?: ""
                                
                                // fromUserName: name > email > uid 순서로 표시
                                val fromUserName = when {
                                    currentUserName.isNotEmpty() -> currentUserName
                                    currentUserEmail.isNotEmpty() -> currentUserEmail
                                    else -> currentUserId
                                }
                                
                                println("GroupScreen: fromUserName determined: $fromUserName")
                                
                                selectedFriends.forEach { friendId ->
                                    println("GroupScreen: Sending group invite to $friendId for group $groupId")
                                    // Firestore groupInvites
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(friendId)
                                        .collection("groupInvites")
                                        .document(groupId)
                                        .set(groupData)
                                    // 알림 전송 (Firestore)
                                    val inviteNotification = mapOf(
                                        "type" to "groupInvite",
                                        "fromUserId" to (currentUserId ?: ""),
                                        "fromUserName" to fromUserName,
                                        "groupId" to groupId,
                                        "groupName" to groupName,
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                    println("GroupScreen: Adding notification to $friendId - type: groupInvite, groupId: $groupId, fromUserName: $fromUserName")
                                    FirebaseFirestore.getInstance().collection("users")
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
                                    showAddMemberDialog = false
                                    selectedFriends = emptySet()
                                    android.widget.Toast.makeText(context, "초대가 전송되었습니다", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                // 에러 처리
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, "초대 전송 중 오류가 발생했습니다: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = selectedFriends.isNotEmpty()
                ) {
                    Text("그룹 초대하기")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddMemberDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("정말로 삭제하시겠습니까?") },
            text = { Text("삭제된 그룹은 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteGroup(group)
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
    
    // 탈퇴 확인 다이얼로그
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("정말 탈퇴하시겠습니까?") },
            text = { 
                Text("더 이상 ${group.name} 그룹에 접근할 수 없습니다.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onLeaveGroup(group)
                }) {
                    Text("탈퇴")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("취소")
                }
            }
        )
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
            val firestore = FirebaseFirestore.getInstance()
            val userDoc = firestore.collection("users").document(memberId).get().await()

            if (userDoc.exists()) {
                memberName = userDoc.getString("name") ?: ""
                memberEmail = userDoc.getString("email") ?: ""
                println("MemberItem: $memberId -> email: $memberEmail, name: $memberName")
            } else {
                println("MemberItem: $memberId -> userDoc does not exist")
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
            // displayText: name > email > uid 순서로 표시
            val displayText = when {
                memberName.isNotEmpty() -> memberName
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

