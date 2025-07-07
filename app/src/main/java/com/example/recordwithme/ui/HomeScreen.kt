package com.example.recordwithme.ui

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.accompanist.flowlayout.FlowRow
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.*

data class Photo(val url: String, val date: String)
data class Group(val id: String, val name: String, val photos: List<Photo> = emptyList())

@Composable
fun HomeScreen() {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser
    val context = LocalContext.current

    var groupList by remember { mutableStateOf<List<Group>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedGroupPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var allPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var currentPhotoIndex by remember { mutableStateOf(0) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            uploadImageToFirebase(selectedImageUri, selectedGroup?.id, firestore, context) { success ->
                if (success) refreshTrigger++
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) galleryLauncher.launch("image/*")
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            try {
                loading = true
                val snapshot = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("groups")
                    .get()
                    .await()
                groupList = snapshot.documents.mapNotNull { doc ->
                    val groupId = doc.id
                    val name = doc.getString("name") ?: return@mapNotNull null
                    Group(groupId, name)
                }
            } catch (_: Exception) {
                groupList = emptyList()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(selectedGroup?.id, refreshTrigger) {
        if (selectedGroup?.id != null) {
            try {
                val snapshot = firestore.collection("groups")
                    .document(selectedGroup!!.id)
                    .collection("photos")
                    .get()
                    .await()
                selectedGroupPhotos = snapshot.documents.mapNotNull { doc ->
                    val url = doc.getString("url") ?: return@mapNotNull null
                    val date = doc.getString("date") ?: "날짜 없음"
                    Photo(url, date)
                }
            } catch (_: Exception) {
                selectedGroupPhotos = emptyList()
            }
        } else {
            selectedGroupPhotos = emptyList()
        }
    }

    LaunchedEffect(groupList, refreshTrigger) {
        if (groupList.isNotEmpty() && selectedGroup == null) {
            try {
                val allPhotosList = mutableListOf<Photo>()
                for (group in groupList) {
                    val snapshot = firestore.collection("groups")
                        .document(group.id)
                        .collection("photos")
                        .get()
                        .await()
                    val groupPhotos = snapshot.documents.mapNotNull { doc ->
                        val url = doc.getString("url") ?: return@mapNotNull null
                        val date = doc.getString("date") ?: "날짜 없음"
                        Photo(url, date)
                    }
                    allPhotosList.addAll(groupPhotos)
                }
                allPhotos = allPhotosList
            } catch (_: Exception) {
                allPhotos = emptyList()
            }
        } else {
            allPhotos = emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (loading) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.LightGray, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {}
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("로딩 중...", color = Color.DarkGray)
                        }
                    }
                } else if (groupList.isEmpty()) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.LightGray, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {}
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("빈 그룹", color = Color.DarkGray)
                        }
                    }
                } else {
                    items(groupList) { group ->
                        val isSelected = group == selectedGroup
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedGroup = if (selectedGroup == group) null else group
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.LightGray, shape = CircleShape)
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, Color(0xFF1976D2), CircleShape) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {}
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(group.name, color = Color.DarkGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val photosToShow = selectedGroupPhotos.ifEmpty { allPhotos }
            
            // 확대 뷰 (팝업창)
            
            
            if (photosToShow.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("가장 먼저 사진을 업로드해보세요!", color = Color.Gray)
                }
            } else {
                val photosByDate = photosToShow.groupBy { it.date }
                val representativePhoto = photosToShow.firstOrNull()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                            Card(modifier = Modifier.fillMaxSize(), elevation = 4.dp) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFE0E0E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (representativePhoto != null) {
                                        AnimatedRepresentativePhoto(representativePhoto.url)
                                    } else {
                                        Text("대표사진 자리", color = Color.Gray)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    photosByDate.forEach { (date, photos) ->
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(date, color = Color.Black, fontSize = 25.sp, modifier = Modifier.padding(start = 16.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.padding(start = 16.dp),
                                mainAxisSpacing = 12.dp,
                                crossAxisSpacing = 12.dp
                            ) {
                                photos.forEach { photo ->
                                    if (photo.url.startsWith("https://")) {
                                        Box(
                                            modifier = Modifier
                                                .size(110.dp)
                                                .background(Color.LightGray)
                                                .clickable { 
                                                    selectedPhotoUrl = photo.url
                                                    currentPhotoIndex = photos.indexOf(photo)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = photo.url,
                                                contentDescription = "사진",
                                                modifier = Modifier.size(110.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier.size(110.dp).background(Color.LightGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("이미지 오류")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedGroup != null) {
            FloatingActionButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                        galleryLauncher.launch("image/*")
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                },
                containerColor = Color(0xFF6D4C41),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).size(64.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "사진 추가", modifier = Modifier.size(32.dp))
            }
        }

        if (selectedPhotoUrl != null) {
            val currentPhotos = selectedGroupPhotos.ifEmpty { allPhotos }
            val currentPhoto = currentPhotos.getOrNull(currentPhotoIndex)
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { selectedPhotoUrl = null }
            ) {
                // 메인 이미지
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = selectedPhotoUrl,
                        contentDescription = "팝업 이미지",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    )
                }
                
                // 상단 버튼들 (저장, 닫기)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                ) {
                    // 저장 버튼
                    IconButton(
                        onClick = { 
                            saveImageToGallery(context, selectedPhotoUrl!!)
                        },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "저장",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 닫기 버튼
                    IconButton(
                        onClick = { selectedPhotoUrl = null },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // 왼쪽 화살표 (이전 사진)
                if (currentPhotoIndex > 0) {
                    IconButton(
                        onClick = {
                            currentPhotoIndex--
                            selectedPhotoUrl = currentPhotos[currentPhotoIndex].url
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(64.dp)
                            .padding(start = 24.dp)
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "이전 사진",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                // 오른쪽 화살표 (다음 사진)
                if (currentPhotoIndex < currentPhotos.size - 1) {
                    IconButton(
                        onClick = {
                            currentPhotoIndex++
                            selectedPhotoUrl = currentPhotos[currentPhotoIndex].url
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(64.dp)
                            .padding(end = 24.dp)
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowRight,
                            contentDescription = "다음 사진",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}

fun saveImageToGallery(context: Context, imageUrl: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(imageUrl))
            .setTitle("사진 저장 중")
            .setDescription("갤러리에 저장됩니다.")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "RecordWithMe_${System.currentTimeMillis()}.jpg")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "이미지 저장을 시작했습니다", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "저장 실패: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// 이미지를 Firebase Storage에 업로드 후 Firestore에 다운로드 URL을 저장하는 함수
private fun uploadImageToFirebase(
    imageUri: Uri,
    groupId: String?,
    firestore: FirebaseFirestore,
    context: Context,
    onSuccess: (Boolean) -> Unit
) {
    if (groupId == null) {
        Toast.makeText(context, "그룹을 선택해주세요", Toast.LENGTH_SHORT).show()
        return
    }

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val dateString = "${year}년 ${month}월 ${day}일"

    // Storage에 업로드
    val storageRef = FirebaseStorage.getInstance().reference
    val fileRef = storageRef.child("group_photos/$groupId/${System.currentTimeMillis()}.jpg")
    val inputStream = context.contentResolver.openInputStream(imageUri)
    if (inputStream == null) {
        Toast.makeText(context, "이미지 파일을 열 수 없습니다", Toast.LENGTH_SHORT).show()
        onSuccess(false)
        return
    }
    val uploadTask = fileRef.putStream(inputStream)
    uploadTask.continueWithTask { task ->
        if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
        fileRef.downloadUrl
    }.addOnSuccessListener { uri ->
        // Firestore에 Storage 다운로드 URL 저장
        val photoData = mapOf(
            "url" to uri.toString(),
            "date" to dateString,
            "uploadedAt" to Timestamp.now()
        )
        firestore.collection("groups")
            .document(groupId)
            .collection("photos")
            .add(photoData)
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    "사진이 성공적으로 추가되었습니다!",
                    Toast.LENGTH_SHORT
                ).show()
                onSuccess(true)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "Firestore 저장 실패: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onSuccess(false)
            }
    }.addOnFailureListener { e ->
        Toast.makeText(
            context,
            "Storage 업로드 실패: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
        onSuccess(false)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedRepresentativePhoto(url: String?) {
    AnimatedContent(
        targetState = url,
        transitionSpec = {
            slideInHorizontally { it } with slideOutHorizontally { -it }
        }
    ) { targetUrl ->
        if (targetUrl != null && targetUrl.startsWith("https://")) {
            AsyncImage(
                model = targetUrl,
                contentDescription = "대표사진",
                modifier = Modifier.size(200.dp)
            )
        } else {
            Text("대표사진 자리", color = Color.Gray)
        }
    }
}
