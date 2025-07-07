package com.example.recordwithme.ui

import android.Manifest
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
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
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.tasks.await
import java.util.*

data class Photo(val url: String, val date: String, val isBase64: Boolean = false)
data class Group(
    val id: String,
    val name: String,
    val photos: List<Photo> = emptyList(),
    val thumbnailUrl: String? = null,
    val thumbnailIsBase64: Boolean = false
)

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
    var thumbnailGroupId by remember { mutableStateOf<String?>(null) }

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

    val thumbnailGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            val groupId = thumbnailGroupId
            if (groupId != null) {
                uploadThumbnailToFirestore(selectedImageUri, groupId, firestore, context) {
                    refreshTrigger++
                }
            }
        }
    }

    LaunchedEffect(currentUser?.uid, refreshTrigger) {
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
                    val groupDoc = firestore.collection("groups").document(groupId).get().await()
                    val thumbnailUrl = groupDoc.getString("thumbnailUrl")
                    val thumbnailIsBase64 = groupDoc.getBoolean("thumbnailIsBase64") ?: false
                    Group(groupId, name, thumbnailUrl = thumbnailUrl, thumbnailIsBase64 = thumbnailIsBase64)
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
                    val isBase64 = doc.getBoolean("isBase64") ?: false
                    Photo(url, date, isBase64)
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
                        val isBase64 = doc.getBoolean("isBase64") ?: false
                        Photo(url, date, isBase64)
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
                            modifier = Modifier
                                .pointerInput(group.id) {
                                    detectTapGestures(
                                        onTap = {
                                            if (selectedGroup == group) {
                                                selectedGroup = null
                                                selectedGroupPhotos = emptyList()
                                            } else {
                                                selectedGroup = group
                                                selectedGroupPhotos = emptyList()
                                            }
                                        },
                                        onLongPress = {
                                            thumbnailGroupId = group.id
                                            thumbnailGalleryLauncher.launch("image/*")
                                        }
                                    )
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
                            ) {
                                if (group.thumbnailUrl != null) {
                                    if (group.thumbnailIsBase64) {
                                        Base64Image(group.thumbnailUrl, Modifier.size(64.dp).clip(CircleShape))
                                    } else if (group.thumbnailUrl.startsWith("https://")) {
                                        AsyncImage(
                                            model = group.thumbnailUrl,
                                            contentDescription = "썸네일",
                                            modifier = Modifier.size(64.dp).clip(CircleShape)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(group.name, color = Color.DarkGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val photosToShow = if (selectedGroup != null) selectedGroupPhotos else allPhotos
            
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
                    .toList()
                    .sortedByDescending { it.first } // 날짜 내림차순 정렬
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
                                        AnimatedRepresentativePhoto(representativePhoto.url, representativePhoto.isBase64)
                                    } else {
                                        Text("대표사진 자리", color = Color.Gray)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    for ((date, photos) in photosByDate) {
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
                                        if (photo.isBase64) {
                                            Base64Image(photo.url, Modifier.size(110.dp))
                                        } else if (photo.url.startsWith("https://")) {
                                            AsyncImage(
                                                model = photo.url,
                                                contentDescription = "사진",
                                                modifier = Modifier.size(110.dp)
                                            )
                                        } else {
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
                    val currentPhoto = currentPhotos.getOrNull(currentPhotoIndex)
                    if (currentPhoto?.isBase64 == true) {
                        Base64Image(
                            selectedPhotoUrl!!,
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        )
                    } else {
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
                            val currentPhoto = currentPhotos.getOrNull(currentPhotoIndex)
                            saveImageToGallery(context, selectedPhotoUrl!!, currentPhoto?.isBase64 ?: false)
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

fun saveImageToGallery(context: Context, imageUrl: String, isBase64: Boolean = false) {
    try {
        if (isBase64) {
            // Base64 이미지를 갤러리에 저장
            val bytes = Base64.decode(imageUrl, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            
            val filename = "RecordWithMe_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let { imageUri ->
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                Toast.makeText(context, "이미지가 갤러리에 저장되었습니다", Toast.LENGTH_SHORT).show()
            }
        } else {
            // URL 이미지를 다운로드
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
        }
    } catch (e: Exception) {
        Toast.makeText(context, "저장 실패: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// 이미지를 Base64로 인코딩하여 Firestore에 저장하는 함수
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

    try {
        // 이미지를 Base64로 인코딩
        val inputStream = context.contentResolver.openInputStream(imageUri)
        if (inputStream == null) {
            Toast.makeText(context, "이미지 파일을 열 수 없습니다", Toast.LENGTH_SHORT).show()
            onSuccess(false)
            return
        }

        val bytes = inputStream.readBytes()
        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
        
        // Firestore에 Base64 데이터 저장
        val photoData = mapOf(
            "url" to base64String,
            "date" to dateString,
            "uploadedAt" to Timestamp.now(),
            "isBase64" to true
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
                    "저장 실패: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onSuccess(false)
            }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "이미지 처리 실패: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
        onSuccess(false)
    }
}

@Composable
fun Base64Image(base64String: String, modifier: Modifier = Modifier) {
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
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier)
    } else {
        Box(modifier = modifier.background(Color.LightGray), contentAlignment = Alignment.Center) {
            Text("이미지 로딩 중...")
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedRepresentativePhoto(url: String?, isBase64: Boolean = false) {
    AnimatedContent(
        targetState = url,
        transitionSpec = {
            slideInHorizontally { it } with slideOutHorizontally { -it }
        }
    ) { targetUrl ->
        if (targetUrl != null) {
            if (isBase64) {
                Base64Image(targetUrl, Modifier.size(200.dp))
            } else if (targetUrl.startsWith("https://")) {
                AsyncImage(
                    model = targetUrl,
                    contentDescription = "대표사진",
                    modifier = Modifier.size(200.dp)
                )
            } else {
                Text("대표사진 자리", color = Color.Gray)
            }
        } else {
            Text("대표사진 자리", color = Color.Gray)
        }
    }
}

// 그룹 썸네일 업로드 함수
private fun uploadThumbnailToFirestore(
    imageUri: Uri,
    groupId: String,
    firestore: FirebaseFirestore,
    context: Context,
    onSuccess: () -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        if (inputStream == null) {
            Toast.makeText(context, "이미지 파일을 열 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        val bytes = inputStream.readBytes()
        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
        val updateMap = mapOf(
            "thumbnailUrl" to base64String,
            "thumbnailIsBase64" to true
        )
        firestore.collection("groups")
            .document(groupId)
            .update(updateMap)
            .addOnSuccessListener {
                Toast.makeText(context, "썸네일이 저장되었습니다!", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "썸네일 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    } catch (e: Exception) {
        Toast.makeText(context, "썸네일 처리 실패: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
