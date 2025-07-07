package com.example.recordwithme.ui

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Rect
import android.widget.EditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import java.io.InputStream
import com.google.firebase.Timestamp

// 사진 데이터 클래스
data class Comment(
    val userId: String = "",
    val text: String = ""
)

data class PhotoData(
    val url: String,
    val isBase64: Boolean,
    val description: String = "",
    val comments: List<Comment> = emptyList()
)

// 그리드 간격 데코레이션 클래스 추가
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount
        outRect.left = spacing - column * spacing / spanCount
        outRect.right = (column + 1) * spacing / spanCount
        if (position < spanCount) {
            outRect.top = spacing
        }
        outRect.bottom = spacing
    }
}

// PhotoAdapter 수정: 다중 뷰타입 지원
class PhotoAdapter(
    private val dateString: String,
    private val photoCount: Int,
    private val photos: List<PhotoData>,
    private val groupId: String,
    private val photoDocIds: List<String>,
    private val onRefresh: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_DATE = 0
        private const val VIEW_TYPE_COUNT = 1
        private const val VIEW_TYPE_PHOTO = 2
    }

    // 날짜 뷰홀더
    class DateViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    // 사진 개수 뷰홀더
    class CountViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    // 사진 뷰홀더(기존)
    class PhotoViewHolder(
        val imageView: ImageView,
        val descView: TextView,
        val commentsView: LinearLayout,
        val commentInput: EditText,
        val commentButton: Button,
        val deleteButton: Button,
        itemView: View
    ) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_DATE
            1 -> VIEW_TYPE_COUNT
            else -> VIEW_TYPE_PHOTO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        return when (viewType) {
            VIEW_TYPE_DATE -> {
                val tv = TextView(context).apply {
                    textSize = 36f
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)
                    setPadding(0, 0, 0, 32)
                }
                DateViewHolder(tv)
            }
            VIEW_TYPE_COUNT -> {
                val tv = TextView(context).apply {
                    textSize = 18f
                    gravity = Gravity.CENTER
                    setTextColor(Color.GRAY)
                    setPadding(0, 0, 0, 32)
                }
                CountViewHolder(tv)
            }
            else -> {
                // 기존 PhotoViewHolder 생성 코드
                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.WHITE)
                    val padding = 32
                    setPadding(padding, padding, padding, padding)
                    val params = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    params.bottomMargin = 32
                    layoutParams = params
                }
                val imageView = ImageView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                }
                val descView = TextView(context).apply {
                    setTextColor(Color.DKGRAY)
                    textSize = 15f
                    setPadding(0, 16, 0, 16)
                }
                val divider = View(context).apply {
                    setBackgroundColor(Color.LTGRAY)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        2
                    )
                }
                val commentsView = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 8, 0, 8)
                }
                val commentInput = EditText(context).apply {
                    hint = "댓글을 입력하세요"
                    textSize = 13f
                }
                val commentButton = Button(context).apply {
                    text = "등록"
                    textSize = 13f
                    setBackgroundColor(Color.BLACK)
                    setTextColor(Color.WHITE)
                }
                val deleteButton = Button(context).apply {
                    text = "삭제"
                    textSize = 13f
                    setBackgroundColor(Color.BLACK)
                    setTextColor(Color.WHITE)
                }
                val deleteParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                deleteParams.topMargin = 12
                deleteButton.layoutParams = deleteParams
                container.addView(imageView)
                container.addView(descView)
                container.addView(divider)
                container.addView(commentsView)
                container.addView(commentInput)
                container.addView(commentButton)
                container.addView(deleteButton)
                PhotoViewHolder(imageView, descView, commentsView, commentInput, commentButton, deleteButton, container)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_DATE -> {
                (holder as DateViewHolder).textView.text = dateString
            }
            VIEW_TYPE_COUNT -> {
                (holder as CountViewHolder).textView.text = "이 날의 사진: ${photoCount}장"
                holder.textView.setTextColor(if (photoCount == 0) Color.GRAY else Color.BLUE)
            }
            VIEW_TYPE_PHOTO -> {
                val photoIdx = position - 2
                val photo = photos[photoIdx]
                val photoHolder = holder as PhotoViewHolder
                // 이하 기존 PhotoAdapter의 onBindViewHolder 내용에서 position -> photoIdx로 변경
                if (photo.isBase64) {
                    try {
                        val bytes = Base64.decode(photo.url, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        photoHolder.imageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        photoHolder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                } else if (photo.url.startsWith("https://")) {
                    photoHolder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
                photoHolder.descView.text = if (photo.description.isBlank()) "+설명" else photo.description
                photoHolder.descView.setTextColor(
                    if (photo.description.isBlank()) Color.parseColor("#1976D2") else Color.DKGRAY
                )
                photoHolder.descView.setOnClickListener {
                    val editText = EditText(photoHolder.descView.context).apply {
                        setText(photo.description)
                        hint = "설명을 입력하세요"
                    }
                    AlertDialog.Builder(photoHolder.descView.context)
                        .setTitle(if (photo.description.isBlank()) "설명 추가" else "설명 수정")
                        .setView(editText)
                        .setPositiveButton("저장") { _, _ ->
                            val newDesc = editText.text.toString().trim()
                            if (newDesc.isNotEmpty() && newDesc != photo.description) {
                                val firestore = FirebaseFirestore.getInstance()
                                val photoId = photoDocIds[photoIdx]
                                val photoDocRef = firestore.collection("groups")
                                    .document(groupId)
                                    .collection("photos")
                                    .document(photoId)
                                photoDocRef.update("description", newDesc)
                                    .addOnSuccessListener { onRefresh() }
                            }
                        }
                        .setNegativeButton("취소", null)
                        .show()
                }
                photoHolder.commentsView.removeAllViews()
                if (photo.comments.isEmpty()) {
                    val emptyView = TextView(photoHolder.commentsView.context).apply {
                        text = "댓글이 없습니다"
                        setTextColor(Color.LTGRAY)
                        textSize = 12f
                    }
                    photoHolder.commentsView.addView(emptyView)
                } else {
                    for (comment in photo.comments) {
                        val commentView = TextView(photoHolder.commentsView.context).apply {
                            text = "${comment.userId} : ${comment.text}"
                            setTextColor(Color.GRAY)
                            textSize = 13f
                            setPadding(0, 4, 0, 4)
                        }
                        photoHolder.commentsView.addView(commentView)
                    }
                }
                photoHolder.commentButton.setOnClickListener {
                    val newCommentText = photoHolder.commentInput.text.toString().trim()
                    if (newCommentText.isNotEmpty()) {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            val usersRef = FirebaseFirestore.getInstance().collection("users")
                            usersRef.document(uid).get().addOnSuccessListener { document ->
                                val userName = document.getString("name") ?: "익명"
                                val firestore = FirebaseFirestore.getInstance()
                                val photoId = photoDocIds[photoIdx]
                                val photoDocRef = firestore.collection("groups")
                                    .document(groupId)
                                    .collection("photos")
                                    .document(photoId)
                                val commentMap = mapOf("userId" to userName, "text" to newCommentText)
                                photoDocRef.update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(commentMap))
                                    .addOnSuccessListener {
                                        photoHolder.commentInput.setText("")
                                        onRefresh()
                                    }
                            }
                        }
                    }
                }
                val context = photoHolder.itemView.context
                val photoId = photoDocIds[photoIdx]
                (photoHolder.itemView as ViewGroup).findViewWithTag<Button>("deleteButton")?.setOnClickListener(null)
                (photoHolder.itemView as ViewGroup).getChildAt((photoHolder.itemView as ViewGroup).childCount - 1).setOnClickListener {
                    AlertDialog.Builder(context)
                        .setTitle("사진 삭제")
                        .setMessage("정말 삭제하시겠습니까?")
                        .setPositiveButton("삭제") { _, _ ->
                            val firestore = FirebaseFirestore.getInstance()
                            firestore.collection("groups")
                                .document(groupId)
                                .collection("photos")
                                .document(photoId)
                                .delete()
                                .addOnSuccessListener { onRefresh() }
                        }
                        .setNegativeButton("취소", null)
                        .show()
                }
            }
        }
    }

    override fun getItemCount(): Int = 2 + photos.size
}

class DayDetailActivity : AppCompatActivity() {
    private lateinit var galleryLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private var groupId: String = ""
    private var year: Int = -1
    private var month: Int = -1
    private var day: Int = -1
    private var groupName: String = ""
    private lateinit var loadPhotosFunc: () -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        day = intent.getIntExtra("day", -1)
        year = intent.getIntExtra("year", -1)
        month = intent.getIntExtra("month", -1)
        groupId = intent.getStringExtra("groupId") ?: ""
        groupName = intent.getStringExtra("groupName") ?: ""
        val receivedTransitionName = intent.getStringExtra("transitionName") ?: ""
        
        // 메인 레이아웃 생성
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(32, 64, 32, 64)
            
            // Shared Element Transition을 위한 transitionName 설정
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                transitionName = receivedTransitionName
            }
        }
        
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val backButton = Button(this).apply {
            text = "◀"
            textSize = 30f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            minHeight = 0
            minWidth = 0
            setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        val space = android.widget.Space(this)
        space.layoutParams = LinearLayout.LayoutParams(0, 0, 1f)

        val addButton = Button(this).apply {
            text = "+"
            textSize = 30f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            minHeight = 0
            minWidth = 0
            setOnClickListener { addPhoto() }
        }
        
        topBar.addView(backButton)
        topBar.addView(space)
        topBar.addView(addButton)
        layout.addView(topBar)
        
        // RecyclerView 생성
        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@DayDetailActivity)
            setBackgroundColor(Color.WHITE)
        }
        layout.addView(recyclerView)
        
        // ActivityResultLauncher 등록 (갤러리에서 사진 선택)
        galleryLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageUri = result.data!!.data
                if (imageUri != null) {
                    selectedImageUri = imageUri
                    uploadImageToFirebase(imageUri)
                }
            }
        }
        
        // 데이터 새로고침 함수 정의
        fun loadPhotos() {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val dateString = "${year}년 ${month}월 ${day}일"
                    val snapshot = firestore.collection("groups")
                        .document(groupId)
                        .collection("photos")
                        .whereEqualTo("date", dateString)
                        .orderBy("uploadedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .get()
                        .await()
                    val photoCount = snapshot.size()
                    val photoList = mutableListOf<PhotoData>()
                    val photoDocIds = mutableListOf<String>()
                    for (document in snapshot.documents) {
                        val imageUrl = document.getString("url") ?: continue
                        val isBase64 = document.getBoolean("isBase64") ?: false
                        val description = document.getString("description") ?: ""
                        val commentsRaw = document.get("comments")
                        val comments: List<Comment> = when (commentsRaw) {
                            is List<*> -> {
                                commentsRaw.map {
                                    when (it) {
                                        is Map<*, *> -> {
                                            val userId = it["userId"]?.toString() ?: "익명"
                                            val text = it["text"]?.toString() ?: ""
                                            Comment(userId, text)
                                        }
                                        is String -> Comment("익명", it)
                                        else -> null
                                    }
                                }.filterNotNull()
                            }
                            else -> emptyList()
                        }
                        photoList.add(PhotoData(imageUrl, isBase64, description, comments))
                        photoDocIds.add(document.id)
                    }
                    recyclerView.adapter = PhotoAdapter(
                        dateString = "${groupName}\n${year}년 ${month}월 ${day}일",
                        photoCount = photoCount,
                        photos = photoList,
                        groupId = groupId,
                        photoDocIds = photoDocIds
                    ) {
                        loadPhotos()
                    }
                } catch (e: Exception) {
                    recyclerView.adapter = PhotoAdapter(
                        dateString = "${groupName}\n${year}년 ${month}월 ${day}일",
                        photoCount = 0,
                        photos = emptyList(),
                        groupId = groupId,
                        photoDocIds = emptyList()
                    ) {
                        loadPhotos()
                    }
                    e.printStackTrace()
                }
            }
        }
        
        // 최초 데이터 로드
        if (groupId.isNotEmpty()) {
            loadPhotos()
        }
        
        loadPhotosFunc = { loadPhotos() }
        
        setContentView(layout)
        
        // Shared Element Transition 설정 - 속도 조절 및 배경 유지
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val transition = android.transition.TransitionInflater.from(this)
                .inflateTransition(android.R.transition.move)
            
            // 애니메이션 지속 시간을 늘려서 더 부드럽게
            transition.duration = 800
            
            window.sharedElementEnterTransition = transition
            
            // 배경이 사라지지 않도록 설정
            window.allowEnterTransitionOverlap = false
            window.allowReturnTransitionOverlap = false
        }
    }

    // 사진 추가 함수 (갤러리에서 선택)
    fun addPhoto() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    // Storage에 업로드 후 Firestore에 저장 → Base64로 Firestore에 직접 저장
    private fun uploadImageToFirebase(imageUri: Uri) {
        val context = this
        val dateString = "${year}년 ${month}월 ${day}일"
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                android.widget.Toast.makeText(context, "이미지 파일을 열 수 없습니다", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            val bytes = inputStream.readBytes()
            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
            val firestore = FirebaseFirestore.getInstance()
            val photoData = hashMapOf(
                "url" to base64String,
                "isBase64" to true,
                "description" to "",
                "comments" to emptyList<String>(),
                "date" to dateString,
                "uploadedAt" to Timestamp.now()
            )
            firestore.collection("groups")
                .document(groupId)
                .collection("photos")
                .add(photoData)
                .addOnSuccessListener {
                    android.widget.Toast.makeText(context, "사진이 추가되었습니다", android.widget.Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    loadPhotosFunc()
                }
                .addOnFailureListener { e ->
                    android.widget.Toast.makeText(context, "Firestore 저장 실패: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "이미지 처리 실패: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}