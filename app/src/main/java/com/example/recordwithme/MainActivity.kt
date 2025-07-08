package com.example.recordwithme

//import NotificationScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.compose.*
import com.example.recordwithme.ui.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.Alignment
import net.openid.appauth.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import android.net.Uri
import android.content.Intent

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val viewModel: AuthViewModel by viewModels()
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private lateinit var authService: AuthorizationService
    private val AUTH_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Google Sign-In Client 초기화
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Firebase Auth 상태 리스너 설정
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            viewModel.setUser(user)
        }

        // 시스템 UI 설정
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUI()

        setContent {
            val navController = rememberNavController()
            val user by viewModel.authenticatedUser.collectAsState()
            val context = LocalContext.current

            var drawerOpen by remember { mutableStateOf(false) }
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route
            val isAuthScreen = currentRoute == "login" || currentRoute == "signup"

            // 다른 탭으로 이동할 때 GroupMode 전역 상태 초기화
            LaunchedEffect(currentRoute) {
                if (currentRoute != "profile" && GroupModeState.isGroupMode) {
                    GroupModeState.isGroupMode = false
                }
            }

            // 권한 상태 관리
            var hasNotificationPermission by remember {
                mutableStateOf(
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                )
            }

            // 권한 요청 런처
            val requestPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                hasNotificationPermission = isGranted
            }

            // 권한 없으면 요청
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // 로그인 시 토스트 메시지 표시
            LaunchedEffect(user?.uid) {
                user?.let { firebaseUser ->
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(firebaseUser.uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val userName = document.getString("name") ?: ""
                                val userId = document.getString("id") ?: ""
                                val displayName =
                                    if (userName.isNotEmpty()) userName else if (userId.isNotEmpty()) "@$userId" else firebaseUser.email ?: "사용자"
                                Toast.makeText(context, "로그인 성공: $displayName", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "로그인 성공: ${firebaseUser.email}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                context,
                                "로그인 성공: ${firebaseUser.email}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }

            // 로그아웃 시 화면 전환
            LaunchedEffect(user) {
                if (user == null && currentRoute != "login") {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            DrawerContainer(
                drawerOpen = drawerOpen,
                onDrawerClose = { drawerOpen = false },
                authViewModel = viewModel
            ) {
                Scaffold(
                    topBar = {
                        if (!isAuthScreen) {
                            TopBar(onMenuClick = { drawerOpen = !drawerOpen })
                        }
                    },
                    bottomBar = {
                        if (!isAuthScreen) {
                            BottomNavigationBar(navController)
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = if (user != null) "home" else "login"
                        ) {
                            composable("login") {
                                LoginScreen(
                                    onLoginSuccess = {
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    navController = navController,
                                    onGoogleSignIn = { signIn() }
                                )
                            }
                            composable("signup") {
                                SignUpScreen(navController = navController)
                            }
                            composable("home") { HomeScreen() }
                            composable("profile") { ProfileScreen(navController = navController) }

                            composable("notification") {
                                if (hasNotificationPermission) {
                                    NotificationScreen()
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("알림 권한이 필요합니다.")
                                    }
                                }
                            }

                            composable("group") { GroupScreen(navController = navController) }
                        }
                    }
                }
            }
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)!!
            viewModel.firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("MainActivity", "Google sign in failed", e)
            viewModel.setUser(null)
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
        )
    }

    fun startSpotifyAuth() {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("https://accounts.spotify.com/authorize"),
            Uri.parse("https://accounts.spotify.com/api/token")
        )
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            "a3809a063b6345fc86e05539e4668b3a", // 본인 Client ID
            ResponseTypeValues.CODE,
            Uri.parse("com.example.recordwithme://callback")
        )
            .setScopes("user-read-private", "user-read-email", "streaming")
            .build()
        authService = AuthorizationService(this)
        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        startActivityForResult(authIntent, AUTH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTH_REQUEST_CODE) {
            val response = AuthorizationResponse.fromIntent(data!!)
            val ex = AuthorizationException.fromIntent(data)
            if (response != null) {
                authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenEx ->
                    authService.dispose()
                    if (tokenResponse != null) {
                        val accessToken = tokenResponse.accessToken
                        val refreshToken = tokenResponse.refreshToken
                        // EncryptedSharedPreferences에 저장
                        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                        val prefs = EncryptedSharedPreferences.create(
                            "spotify_prefs",
                            masterKeyAlias,
                            applicationContext,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                        prefs.edit().putString("access_token", accessToken).apply()
                        prefs.edit().putString("refresh_token", refreshToken).apply()
                        runOnUiThread {
                            Toast.makeText(this, "AccessToken 저장 완료!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "토큰 교환 실패: ${tokenEx?.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else if (ex != null) {
                Toast.makeText(this, "인증 실패: ${ex.errorDescription}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "인증 결과 없음", Toast.LENGTH_LONG).show()
            }
        }
    }
}
