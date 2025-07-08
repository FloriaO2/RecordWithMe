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
import java.io.InputStream
import com.google.firebase.Timestamp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject
import org.json.JSONArray
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.media.MediaPlayer
import android.widget.ImageButton
import android.util.Log
import com.example.recordwithme.BuildConfig

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

    // 현재 재생 중인 MediaPlayer
    private var currentMediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int = -1

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
        val labelButton: Button,
        val musicOverlay: LinearLayout,
        val musicText: TextView,
        val playButton: ImageButton,
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
                val container = FrameLayout(context).apply {
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
                
                // 내부 컨테이너 (기존 LinearLayout)
                val innerContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                
                val imageView = ImageView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                }
                
                // 음악 정보 오버레이 (처음에는 숨김)
                val musicText = TextView(context).apply {
                    text = "🎵 이 순간과 어울리는 음악은 무엇일까요?"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 16, 0)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.85f)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setShadowLayer(4f, 0f, 0f, Color.BLACK)
                    maxWidth = (180 * context.resources.displayMetrics.density).toInt() // 180dp 제한
                }
                val playButton = ImageButton(context).apply {
                    setImageResource(android.R.drawable.ic_media_play)
                    setBackgroundColor(Color.TRANSPARENT)
                    setColorFilter(Color.BLACK)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.15f)
                    visibility = View.GONE // 처음엔 안 보이게
                }
                val musicOverlay = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundColor(Color.TRANSPARENT)
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(24, 16, 24, 16)
                    weightSum = 1f
                    visibility = View.VISIBLE // 항상 보이게
                    addView(musicText)
                    addView(playButton)
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
                val buttonSpacer = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        12 // 등록-삭제 버튼과 동일하게 12px 높이로 변경
                    )
                }
                val labelButton = Button(context).apply {
                    text = "어울리는 음악 재생"
                    textSize = 13f
                    setBackgroundColor(Color.parseColor("#1976D2"))
                    setTextColor(Color.WHITE)
                }
                val deleteParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                deleteParams.topMargin = 12
                deleteButton.layoutParams = deleteParams
                innerContainer.addView(imageView)
                innerContainer.addView(musicOverlay)
                innerContainer.addView(descView)
                innerContainer.addView(divider)
                innerContainer.addView(commentsView)
                innerContainer.addView(commentInput)
                innerContainer.addView(commentButton)
                innerContainer.addView(deleteButton)
                innerContainer.addView(buttonSpacer)
                innerContainer.addView(labelButton)
                
                container.addView(innerContainer)
                
                PhotoViewHolder(imageView, descView, commentsView, commentInput, commentButton, deleteButton, labelButton, musicOverlay, musicText, playButton, container)
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
                // 항상 초기화
                photoHolder.musicText.text = "🎵 이 순간과 어울리는 음악은 무엇일까요?"
                photoHolder.playButton.visibility = View.GONE
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
                photoHolder.deleteButton.setOnClickListener {
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
                // 라벨 추출 버튼 클릭 리스너
                photoHolder.labelButton.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                photoHolder.musicOverlay.visibility = View.VISIBLE
                            }
                            val visionApiKey = BuildConfig.VISION_API_KEY
                            val labels = com.example.recordwithme.util.VisionApiHelper.getLabelsFromVisionApi(
                                photo.url, // Base64 데이터
                                visionApiKey
                            )
                            Log.d("SpotifyDebug", "Vision 라벨: $labels")
                            if (labels.isNotEmpty()) {
                                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                                val prefs = EncryptedSharedPreferences.create(
                                    "spotify_prefs",
                                    masterKeyAlias,
                                    photoHolder.itemView.context,
                                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                                )
                                val accessToken = prefs.getString("access_token", null)
                                if (accessToken != null) {
                                    val musicKeywords = listOf("k-pop", "Korean", "노래", "music")
                                    val searchQuery = (labels.take(3) + musicKeywords).joinToString(" ")
                                    val tracks = searchSpotifyTracks(searchQuery, accessToken)
                                    Log.d("SpotifyDebug", "검색 쿼리: $searchQuery, 트랙 수: ${tracks.size}")
                                    if (tracks.isNotEmpty()) {
                                        val playableTrack = tracks.firstOrNull { it.previewUrl != null }
                                        if (playableTrack != null) {
                                            Log.d("SpotifyDebug", "첫 미리듣기 곡: ${playableTrack.name}, previewUrl: ${playableTrack.previewUrl}")
                                            (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                                val musicText = photoHolder.musicText
                                                musicText.text = "🎵 ${playableTrack.name} - ${playableTrack.artist}"
                                                val playButton = photoHolder.playButton
                                                playButton.setImageResource(android.R.drawable.ic_media_pause)
                                                playButton.visibility = View.VISIBLE
                                                playButton.setOnClickListener {
                                                    togglePlayPause(playableTrack.previewUrl, photoHolder, photoIdx)
                                                }
                                                photoHolder.itemView.tag = playableTrack
                                                // 자동 재생
                                                playPreviewUrl(playableTrack.previewUrl, photoHolder, photoIdx, autoPlay = true)
                                            }
                                        } else {
                                            // iTunes에서 미리듣기 URL 시도
                                            val mostPopularTrack = tracks.maxByOrNull { it.popularity }
                                            if (mostPopularTrack != null) {
                                                Log.d("SpotifyDebug", "iTunes 검색용 곡 정보(인기순): name=${mostPopularTrack.name}, artist=${mostPopularTrack.artist}, popularity=${mostPopularTrack.popularity}")
                                                val itunesPreviewUrl = getItunesPreviewUrl(mostPopularTrack.name, mostPopularTrack.artist)
                                                if (itunesPreviewUrl != null) {
                                                    Log.d("SpotifyDebug", "iTunes 미리듣기 URL: $itunesPreviewUrl")
                                                    (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                                        val musicText = photoHolder.musicText
                                                        musicText.text = "🎵 ${mostPopularTrack.name} - ${mostPopularTrack.artist} (iTunes)"
                                                        val playButton = photoHolder.playButton
                                                        playButton.setImageResource(android.R.drawable.ic_media_pause)
                                                        playButton.visibility = View.VISIBLE
                                                        playButton.setOnClickListener {
                                                            togglePlayPause(itunesPreviewUrl, photoHolder, photoIdx)
                                                        }
                                                        photoHolder.itemView.tag = mostPopularTrack
                                                        // 자동 재생
                                                        playPreviewUrl(itunesPreviewUrl, photoHolder, photoIdx, autoPlay = true)
                                                    }
                                                } else {
                                                    (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                                        photoHolder.musicOverlay.visibility = View.GONE
                                                        android.widget.Toast.makeText(
                                                            photoHolder.itemView.context,
                                                            "미리듣기 가능한 곡이 없습니다.",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            } else {
                                                (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                                    photoHolder.musicOverlay.visibility = View.GONE
                                                    android.widget.Toast.makeText(
                                                        photoHolder.itemView.context,
                                                        "미리듣기 가능한 곡이 없습니다.",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    } else {
                                        (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                            photoHolder.musicOverlay.visibility = View.GONE
                                            android.widget.Toast.makeText(
                                                photoHolder.itemView.context,
                                                "검색 결과가 없습니다.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                } else {
                                    (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                        photoHolder.musicOverlay.visibility = View.GONE
                                        android.widget.Toast.makeText(
                                            photoHolder.itemView.context,
                                            "Spotify 로그인이 필요합니다.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } else {
                                (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                    photoHolder.musicOverlay.visibility = View.GONE
                                    android.widget.Toast.makeText(
                                        photoHolder.itemView.context,
                                        "라벨을 추출할 수 없습니다.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            (photoHolder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                photoHolder.musicOverlay.visibility = View.GONE
                                android.widget.Toast.makeText(
                                    photoHolder.itemView.context,
                                    "오류: ${e.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = 2 + photos.size

    // Preview URL 재생 함수
    private fun playPreviewUrl(previewUrl: String?, photoHolder: PhotoViewHolder, photoIdx: Int, autoPlay: Boolean = false) {
        Log.d("SpotifyDebug", "playPreviewUrl 진입, previewUrl: $previewUrl, autoPlay: $autoPlay")
        if (previewUrl == null) {
            // Preview URL이 없으면 Spotify 앱으로 이동
            val track = photoHolder.itemView.tag as? Track
            if (track != null) {
                val spotifyUri = "spotify:track:${track.id}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    photoHolder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        photoHolder.itemView.context,
                        "Spotify 앱이 설치되지 않았습니다.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            return
        }
        
        // 이전 재생 중인 음악 정지
        stopCurrentMusic()
        
        try {
            currentMediaPlayer = MediaPlayer().apply {
                // 오디오 스트림 타입 설정 (미디어 볼륨)
                setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
                
                // 볼륨 설정 (최대 볼륨의 80%)
                val audioManager = photoHolder.itemView.context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                val targetVolume = (maxVolume * 0.8).toInt()
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVolume, 0)
                
                setDataSource(previewUrl)
                prepareAsync()
                setOnPreparedListener { player ->
                    Log.d("SpotifyDebug", "MediaPlayer 준비 완료, 재생 시작")
                    if (autoPlay) {
                        player.start()
                        currentPlayingPosition = photoIdx
                        val playButton = photoHolder.playButton
                        playButton.setImageResource(android.R.drawable.ic_media_pause)
                    }
                }
                setOnCompletionListener { player ->
                    // 재생 완료 시 재생 버튼으로 변경
                    val playButton = photoHolder.playButton
                    playButton.setImageResource(android.R.drawable.ic_media_play)
                    currentPlayingPosition = -1
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("SpotifyDebug", "MediaPlayer 에러 발생 what=$what, extra=$extra")
                    android.widget.Toast.makeText(
                        photoHolder.itemView.context,
                        "재생 중 오류가 발생했습니다. (코드: $what, $extra)",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    val playButton = photoHolder.playButton
                    playButton.setImageResource(android.R.drawable.ic_media_play)
                    currentPlayingPosition = -1
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("SpotifyDebug", "MediaPlayer 예외: ${e.message}")
            android.widget.Toast.makeText(
                photoHolder.itemView.context,
                "재생 준비 중 오류가 발생했습니다: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        
        Log.d("SpotifyPreview", "미리듣기 URL: $previewUrl")
    }
    
    // 현재 재생 중인 음악 정지
    private fun stopCurrentMusic() {
        currentMediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        currentMediaPlayer = null
        currentPlayingPosition = -1
    }
    
    // Adapter 소멸 시 MediaPlayer 정리
    fun cleanup() {
        stopCurrentMusic()
    }

    // 일시정지/이어듣기 토글 함수 (반드시 클래스 내부에 위치)
    private fun togglePlayPause(previewUrl: String?, photoHolder: PhotoViewHolder, photoIdx: Int) {
        if (currentMediaPlayer != null && currentMediaPlayer!!.isPlaying) {
            currentMediaPlayer?.pause()
            val playButton = photoHolder.playButton
            playButton.setImageResource(android.R.drawable.ic_media_play)
        } else if (currentMediaPlayer != null && !currentMediaPlayer!!.isPlaying) {
            currentMediaPlayer?.start()
            val playButton = photoHolder.playButton
            playButton.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            // 처음 재생
            playPreviewUrl(previewUrl, photoHolder, photoIdx, autoPlay = true)
        }
    }
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
    private var photoAdapter: PhotoAdapter? = null

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
                    photoAdapter = recyclerView.adapter as PhotoAdapter
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
                    photoAdapter = recyclerView.adapter as PhotoAdapter
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

    override fun onDestroy() {
        super.onDestroy()
        photoAdapter?.cleanup()
    }
}

// Spotify Track 데이터 클래스
data class Track(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val previewUrl: String?,
    val popularity: Int = 0
)

// Spotify API로 음악 검색하는 함수 (한국 음악 우선, 유명한 곡들)
private fun searchSpotifyTracks(query: String, accessToken: String): List<Track> {
    return try {
        // 한국 음악으로 검색 범위 제한 (장르: k-pop, korean, 한국어)
        val koreanQuery = "$query korean k-pop"
        val encodedQuery = java.net.URLEncoder.encode(koreanQuery, "UTF-8")
        val url = URL("https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=10&market=US")
        
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("SpotifyDebug", "Spotify API 응답: $response")
            val jsonObject = JSONObject(response)
            val tracksObject = jsonObject.getJSONObject("tracks")
            val itemsArray = tracksObject.getJSONArray("items")
            
            val tracks = mutableListOf<Track>()
            for (i in 0 until itemsArray.length()) {
                val trackObject = itemsArray.getJSONObject(i)
                val id = trackObject.getString("id")
                val name = trackObject.getString("name")
                val previewUrl = if (trackObject.has("preview_url") && !trackObject.isNull("preview_url")) {
                    trackObject.getString("preview_url")
                } else null
                
                val artist = try {
                    val artistsArray = trackObject.getJSONArray("artists")
                    if (artistsArray.length() > 0) {
                        artistsArray.getJSONObject(0).getString("name")
                    } else {
                        "Unknown Artist"
                    }
                } catch (e: Exception) {
                    "Unknown Artist"
                }
                
                val albumObject = trackObject.getJSONObject("album")
                val album = albumObject.getString("name")
                val popularity = trackObject.optInt("popularity", 0)
                
                tracks.add(Track(id, name, artist, album, previewUrl, popularity))
            }
            
            // 인기도 순으로 정렬 (유명한 곡 우선)
            tracks.sortedByDescending { track ->
                // Track 클래스에 popularity 필드가 없으므로 이름으로 유명도 추정
                val popularKeywords = listOf("방탄소년단", "BTS", "블랙핑크", "BLACKPINK", "아이유", "IU", 
                    "세븐틴", "SEVENTEEN", "트와이스", "TWICE", "레드벨벳", "Red Velvet", "엑소", "EXO",
                    "뉴진스", "NewJeans", "르세라핌", "LE SSERAFIM", "아이브", "IVE", "스테이씨", "STAYC")
                
                popularKeywords.count { keyword ->
                    track.name.contains(keyword, ignoreCase = true) || 
                    track.artist.contains(keyword, ignoreCase = true)
                } + track.popularity
            }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

// iTunes Search API로 미리듣기 URL을 받아오는 함수
private fun getItunesPreviewUrl(trackName: String, artist: String): String? {
    return try {
        val query = java.net.URLEncoder.encode("$trackName $artist", "UTF-8")
        val url = URL("https://itunes.apple.com/search?term=$query&entity=song&limit=1")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val results = json.getJSONArray("results")
            if (results.length() > 0) {
                val first = results.getJSONObject(0)
                return first.getString("previewUrl")
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}